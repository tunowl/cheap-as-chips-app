package com.example.cheapaschip.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.os.Build
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseService {
    private const val TAG = "SupabaseService"
    private const val PREFS_NAME = "supabase_prefs"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_key"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    var supabaseUrl: String = "https://afsdocrgmqmibwcgqeuh.supabase.co"
    var supabaseAnonKey: String = "sb_publishable_0Bluq81vydTnf3S5TzlpvQ_PNVKR36N"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawUrl = prefs.getString(KEY_URL, "") ?: ""
        val savedKey = prefs.getString(KEY_ANON_KEY, "") ?: ""
        
        if (rawUrl.isNotBlank() && savedKey.isNotBlank()) {
            supabaseUrl = rawUrl.trim()
                .removeSuffix("/")
                .removeSuffix("/rest/v1")
                .removeSuffix("/")
            supabaseAnonKey = savedKey
        }
    }

    // Helper to upload a single photo to Supabase storage bucket
    fun uploadPhoto(bucket: String, path: String, byteArray: ByteArray): String? {
        if (!isConfigured()) return null
        val url = "$supabaseUrl/storage/v1/object/$bucket/$path"
        val body = byteArray.toRequestBody("image/jpeg".toMediaType())
        val builder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .addHeader("Content-Type", "image/jpeg")

        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful || response.code == 400) {
                    // Return public URL of the uploaded image
                    "$supabaseUrl/storage/v1/object/public/$bucket/$path"
                } else {
                    Log.e(TAG, "Upload failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            null
        }
    }

    // Helper to convert Uri or Bitmap or String Uri to JPEG ByteArray
    fun getBytesFromImage(context: Context, image: Any?): ByteArray? {
        if (image == null) return null
        return try {
            when (image) {
                is android.graphics.Bitmap -> {
                    val stream = java.io.ByteArrayOutputStream()
                    image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                }
                is Uri -> {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, image)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, image)
                    }
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                }
                is String -> {
                    if (image.startsWith("http")) return null
                    val uri = Uri.parse(image)
                    getBytesFromImage(context, uri)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bytes from image", e)
            null
        }
    }

    // Helper to upload list of images (represented as Bitmap or Uri or URL String)
    fun uploadImagesAndGetUrls(bucket: String, prefix: String, images: List<Any?>): String {
        val context = appContext ?: return ""
        val urls = mutableListOf<String>()
        images.filterNotNull().forEachIndexed { index, image ->
            val imgStr = when (image) {
                is String -> image
                is Uri -> image.toString()
                else -> ""
            }
            if (imgStr.startsWith("http")) {
                urls.add(imgStr)
            } else {
                val bytes = getBytesFromImage(context, image)
                if (bytes != null) {
                    val filename = "${prefix}_${System.currentTimeMillis()}_$index.jpg"
                    val publicUrl = uploadPhoto(bucket, filename, bytes)
                    if (publicUrl != null) {
                        urls.add(publicUrl)
                    }
                }
            }
        }
        return urls.joinToString(",")
    }

    fun saveConfig(context: Context, url: String, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanUrl = url.trim()
            .removeSuffix("/")
            .removeSuffix("/rest/v1")
            .removeSuffix("/")
        prefs.edit()
            .putString(KEY_URL, cleanUrl)
            .putString(KEY_ANON_KEY, key)
            .apply()
        supabaseUrl = cleanUrl
        supabaseAnonKey = key
    }

    fun isConfigured(): Boolean {
        return supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
    }

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "apikey" to supabaseAnonKey,
            "Authorization" to "Bearer $supabaseAnonKey",
            "Content-Type" to "application/json",
            "Prefer" to "return=representation"
        )
    }

    // GET Request
    private fun get(path: String): String? {
        if (!isConfigured()) return null
        val url = "$supabaseUrl/rest/v1/$path"
        val builder = Request.Builder().url(url)
        getHeaders().forEach { (k, v) -> builder.addHeader(k, v) }
        
        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "GET failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET error", e)
            null
        }
    }

    // POST Request
    private fun post(path: String, json: String): String? {
        if (!isConfigured()) return null
        val url = "$supabaseUrl/rest/v1/$path"
        val body = json.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder().url(url).post(body)
        getHeaders().forEach { (k, v) -> builder.addHeader(k, v) }

        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "POST failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST error", e)
            null
        }
    }

    // PATCH Request
    private fun patch(path: String, json: String): String? {
        if (!isConfigured()) return null
        val url = "$supabaseUrl/rest/v1/$path"
        val body = json.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder().url(url).patch(body)
        getHeaders().forEach { (k, v) -> builder.addHeader(k, v) }

        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "PATCH failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PATCH error", e)
            null
        }
    }

    // DELETE Request
    private fun delete(path: String): String? {
        if (!isConfigured()) return null
        val url = "$supabaseUrl/rest/v1/$path"
        val builder = Request.Builder().url(url).delete()
        getHeaders().forEach { (k, v) -> builder.addHeader(k, v) }

        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "DELETE failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DELETE error", e)
            null
        }
    }

    // --- API Operations ---

    fun deleteMotor(id: String, plateNumber: String = ""): Boolean {
        if (!isConfigured()) return false
        return try {
            val query = if (id.all { it.isDigit() }) "id=eq.$id" else "plate_number=eq.$plateNumber"
            val res = delete("motor?$query")
            res != null
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting motor", e)
            false
        }
    }

    fun getDefaultPriceForModel(model: String, cc: Int): Double {
        val modelLower = model.lowercase()
        return when {
            modelLower.contains("forza") || modelLower.contains("xmax") || cc >= 300 -> 1200.0
            modelLower.contains("adv 350") -> 1200.0
            modelLower.contains("adv") -> 700.0
            modelLower.contains("pcx 160") -> 600.0
            modelLower.contains("pcx") || modelLower.contains("click 160") -> 500.0
            else -> 400.0
        }
    }

    fun fetchMotors(): List<Scooter>? {
        val jsonStr = get("motor?select=*,model(*)") ?: return null
        val list = mutableListOf<Scooter>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val idStr = obj.optLong("id").toString()
                
                val modelObj = obj.optJSONObject("model")
                val brand = modelObj?.optString("brand", "Honda") ?: "Honda"
                val model = modelObj?.optString("model", "") ?: ""
                val cc = modelObj?.optInt("cc", 125) ?: 125
                val year = modelObj?.optInt("year", 2023) ?: 2023
                
                val plateNumber = obj.optString("plate_number", "")
                val statusStr = obj.optString("status", "AVAILABLE")
                val status = if (statusStr.equals("RENTED", ignoreCase = true)) ScooterStatus.RENTED else ScooterStatus.AVAILABLE
                val photo = obj.optString("photo", "")
                val imagesList = if (photo.isNotEmpty()) {
                    photo.split(",").map { Uri.parse(it.trim()) }
                } else emptyList()

                // Read prices from modelObj
                val priceDaily = modelObj?.optDouble("price_daily", 400.0) ?: 400.0
                val price3Day = modelObj?.optDouble("price_3day", priceDaily * 3.0 * 0.9) ?: (priceDaily * 3.0 * 0.9)
                val priceWeekly = modelObj?.optDouble("price_weekly", priceDaily * 7.0 * 0.85) ?: (priceDaily * 7.0 * 0.85)
                val price2Week = modelObj?.optDouble("price_2week", priceDaily * 14.0 * 0.75) ?: (priceDaily * 14.0 * 0.75)
                val priceMonthly = modelObj?.optDouble("price_monthly", priceDaily * 30.0 * 0.65) ?: (priceDaily * 30.0 * 0.65)
                val depositAmount = modelObj?.optDouble("deposit_amount", if (cc >= 300) 10000.0 else 3000.0) ?: (if (cc >= 300) 10000.0 else 3000.0)
                val description = modelObj?.optString("description", "Premium commuter scooter.") ?: "Premium commuter scooter."

                // Read features from modelObj feature column
                val featureStr = modelObj?.optString("feature", "") ?: ""
                val featuresList = featureStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val hasPhoneCharger = featuresList.any { it.contains("charger", ignoreCase = true) || it.contains("phone", ignoreCase = true) }
                val hasKeyless = featuresList.any { it.contains("keyless", ignoreCase = true) }
                val hasAbs = featuresList.any { it.contains("abs", ignoreCase = true) }

                // fuel_bars and fuel_cost_per_bar are on the motor itself
                val fuelBars = obj.optInt("fuel_bars", modelObj?.optInt("fuel_bars", 6) ?: 6)
                val fuelCostPerBar = obj.optDouble("fuel_cost_per_bar", modelObj?.optDouble("fuel_cost_per_bar", 100.0) ?: 100.0)

                val scooter = Scooter(
                    id = idStr,
                    model = model,
                    brand = brand,
                    plateNumber = plateNumber,
                    pricePerDay = priceDaily,
                    status = status,
                    description = description,
                    cc = cc,
                    year = year,
                    hasPhoneCharger = hasPhoneCharger,
                    hasKeyless = hasKeyless,
                    hasAbs = hasAbs,
                    priceDaily = priceDaily,
                    price3Day = price3Day,
                    priceWeekly = priceWeekly,
                    price2Week = price2Week,
                    priceMonthly = priceMonthly,
                    depositAmount = depositAmount,
                    images = imagesList,
                    fuelBars = fuelBars,
                    fuelCostPerBar = fuelCostPerBar
                )
                list.add(scooter)
            }
            return list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing motors", e)
            return null
        }
    }

    fun fetchContracts(): List<RentalContract>? {
        val jsonStr = get("shop?select=*,customer(*),motor(*,model(*))") ?: return null
        val list = mutableListOf<RentalContract>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val idStr = obj.optLong("id").toString()
                
                val customerObj = obj.optJSONObject("customer")
                val motorObj = obj.optJSONObject("motor")
                val modelObj = motorObj?.optJSONObject("model")
                
                val customerName = customerObj?.optString("name", "") ?: ""
                val customerPhone = customerObj?.optString("phone_number", "") ?: ""
                val customerEmail = customerObj?.optString("email", "") ?: ""
                val customerWhatsApp = customerObj?.optString("whatsapp_number", "") ?: ""
                val passportNumber = customerObj?.optString("passport_id", "") ?: ""
                val licenseNumber = customerObj?.optString("license_id", "") ?: ""
                val hotelAddress = customerObj?.optString("address", "") ?: ""
                
                val scooterIdStr = motorObj?.optLong("id")?.toString() ?: ""
                
                val startDate = obj.optString("start_date", "")
                val dueDateStr = obj.optString("due_date", "")
                val deposit = obj.optDouble("deposit", 3000.0)
                val originalPassport = obj.optBoolean("original_passport", false)
                val helmetCount = obj.optInt("helmet_count", 1)
                
                // Calculate days
                val days = try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val start = sdf.parse(startDate)
                    val due = sdf.parse(dueDateStr)
                    if (start != null && due != null) {
                        val diff = due.time - start.time
                        java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS).toInt()
                    } else 1
                } catch (e: Exception) {
                    1
                }
                
                // Try to get total_price from shop table, or fall back to calculating it dynamically
                val priceFromDb = obj.optDouble("total_price", 0.0)
                val finalTotalPrice = if (priceFromDb > 0.0) {
                    priceFromDb
                } else {
                    val motorDailyRaw = modelObj?.optDouble("price_daily", 0.0) ?: 0.0
                    var motorDaily = if (motorDailyRaw > 0.0) motorDailyRaw else 0.0
                    if (motorDaily <= 0.0 && modelObj != null) {
                        val mModel = modelObj.optString("model", "")
                        val mCc = modelObj.optInt("cc", 125)
                        motorDaily = getDefaultPriceForModel(mModel, mCc)
                    }
                    if (motorDaily <= 0.0) motorDaily = 400.0
 
                    val motor3DayRaw = modelObj?.optDouble("price_3day", 0.0) ?: 0.0
                    val motor3Day = if (motor3DayRaw > 0.0) motor3DayRaw else motorDaily * 3.0 * 0.9
 
                    val motorWeeklyRaw = modelObj?.optDouble("price_weekly", 0.0) ?: 0.0
                    val motorWeekly = if (motorWeeklyRaw > 0.0) motorWeeklyRaw else motorDaily * 7.0 * 0.85
 
                    val motor2WeekRaw = modelObj?.optDouble("price_2week", 0.0) ?: 0.0
                    val motor2Week = if (motor2WeekRaw > 0.0) motor2WeekRaw else motorDaily * 14.0 * 0.75
 
                    val motorMonthlyRaw = modelObj?.optDouble("price_monthly", 0.0) ?: 0.0
                    val motorMonthly = if (motorMonthlyRaw > 0.0) motorMonthlyRaw else motorDaily * 30.0 * 0.65
 
                    val cDays = if (days > 0) days else 1
                    when {
                        cDays >= 30 -> motorMonthly
                        cDays >= 14 -> motor2Week
                        cDays >= 7 -> motorWeekly
                        cDays >= 3 -> motor3Day
                        else -> motorDaily * cDays
                    }
                }
 
                val isActive = obj.optBoolean("is_active", true)
                val passportPhoto = customerObj?.optString("passport_photo", "") ?: ""
                val licensePhoto = customerObj?.optString("license_photo", "") ?: ""
                val scratchPhotosStr = obj.optString("scratch_photos", "") ?: ""
 
                val contract = RentalContract(
                    id = idStr,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    licenseNumber = licenseNumber,
                    scooterId = scooterIdStr,
                    days = if (days > 0) days else 1,
                    totalPrice = finalTotalPrice,
                    depositAmount = deposit,
                    startDate = startDate,
                    isCompleted = !isActive,
                    customerEmail = customerEmail,
                    customerWhatsApp = customerWhatsApp,
                    nationality = "",
                    passportNumber = passportNumber,
                    hotelAddress = hotelAddress,
                    roomNumber = "",
                    passportHold = originalPassport,
                    helmetCount = helmetCount,
                    passportImage = if (passportPhoto.isNotEmpty()) android.net.Uri.parse(passportPhoto) else null,
                    licenseImage = if (licensePhoto.isNotEmpty()) android.net.Uri.parse(licensePhoto) else null,
                    scratchImages = if (scratchPhotosStr.isNotEmpty()) {
                        scratchPhotosStr.split(",").map { android.net.Uri.parse(it.trim()) }
                    } else emptyList()
                )
                list.add(contract)
            }
            return list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing contracts", e)
            return null
        }
    }

    fun saveContract(contract: RentalContract, scooter: Scooter): Boolean {
        if (!isConfigured()) return false
        
        try {
            // 1. Get or create customer in Supabase
            val passport = contract.passportNumber.ifBlank { "P-${contract.licenseNumber.ifBlank { java.util.UUID.randomUUID().toString().take(8) }}" }
            val license = contract.licenseNumber.ifBlank { "L-${contract.passportNumber.ifBlank { java.util.UUID.randomUUID().toString().take(8) }}" }
            
            // Upload customer documents and scratch photos to rentals bucket
            val passportUrl = uploadImagesAndGetUrls("rentals", "passport_${passport}", listOf(contract.passportImage))
            val licenseUrl = uploadImagesAndGetUrls("rentals", "license_${license}", listOf(contract.licenseImage))
            val scratchUrls = uploadImagesAndGetUrls("rentals", "scratch_${contract.id}", contract.scratchImages)

            var customerId: Long? = null
            val queryCust = get("customer?passport_id=eq.$passport&select=id")
            if (!queryCust.isNullOrBlank()) {
                val arr = JSONArray(queryCust)
                if (arr.length() > 0) {
                    customerId = arr.getJSONObject(0).optLong("id")
                }
            }
            
            if (customerId == null) {
                val custJson = JSONObject().apply {
                    put("name", contract.customerName)
                    put("passport_id", passport)
                    put("license_id", license)
                    put("address", contract.hotelAddress)
                    put("phone_number", contract.customerPhone)
                    put("email", contract.customerEmail)
                    put("whatsapp_number", contract.customerWhatsApp)
                    if (passportUrl.isNotEmpty()) {
                        put("passport_photo", passportUrl)
                    }
                    if (licenseUrl.isNotEmpty()) {
                        put("license_photo", licenseUrl)
                    }
                }.toString()
                
                val res = post("customer", custJson) ?: return false
                val arr = JSONArray(res)
                if (arr.length() > 0) {
                    customerId = arr.getJSONObject(0).optLong("id")
                }
            } else {
                // If customer exists, update passport/license photos if provided
                if (passportUrl.isNotEmpty() || licenseUrl.isNotEmpty()) {
                    val updateJson = JSONObject().apply {
                        if (passportUrl.isNotEmpty()) put("passport_photo", passportUrl)
                        if (licenseUrl.isNotEmpty()) put("license_photo", licenseUrl)
                    }.toString()
                    patch("customer?id=eq.$customerId", updateJson)
                }
            }
            
            if (customerId == null) return false
            
            // 2. Get motor ID in Supabase
            var motorId: Long? = scooter.id.toLongOrNull()
            val queryMotor = get("motor?plate_number=eq.${scooter.plateNumber}&select=id")
            if (!queryMotor.isNullOrBlank()) {
                val arr = JSONArray(queryMotor)
                if (arr.length() > 0) {
                    motorId = arr.getJSONObject(0).optLong("id")
                }
            }
            
            if (motorId == null) {
                val savedMotorIdStr = saveScooterAndGetId(scooter)
                if (savedMotorIdStr != null) {
                    motorId = savedMotorIdStr.toLongOrNull()
                }
            }
            
            if (motorId == null) return false
            
            // 3. Update motor status to RENTED
            patch("motor?id=eq.$motorId", JSONObject().put("status", "RENTED").toString())
            
            // 5. Insert into shop table
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startStr = if (contract.startDate.contains(" ")) contract.startDate.split(" ")[0] else contract.startDate
            val cal = java.util.Calendar.getInstance()
            try {
                cal.time = sdf.parse(startStr) ?: java.util.Date()
            } catch (e: Exception) {
                cal.time = java.util.Date()
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, contract.days)
            val dueStr = sdf.format(cal.time)
            
            val shopJson = JSONObject().apply {
                put("customer_id", customerId)
                put("motor_id", motorId)
                put("start_date", startStr)
                put("due_date", dueStr)
                put("deposit", contract.depositAmount)
                put("original_passport", contract.passportHold)
                put("helmet_count", contract.helmetCount)
                put("is_active", true)
                put("total_price", contract.totalPrice)
                if (scratchUrls.isNotEmpty()) {
                    put("scratch_photos", scratchUrls)
                }
            }.toString()
            
            post("shop", shopJson) ?: return false
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving contract", e)
            return false
        }
    }

    fun returnScooter(plateNumber: String): Boolean {
        if (!isConfigured()) return false
        try {
            val queryMotor = get("motor?plate_number=eq.$plateNumber&select=id") ?: return false
            val arr = JSONArray(queryMotor)
            if (arr.length() > 0) {
                val motorId = arr.getJSONObject(0).optLong("id")
                patch("motor?id=eq.$motorId", JSONObject().put("status", "AVAILABLE").toString())
                
                // Set is_active = false for the active shop record of this motor
                val queryShop = get("shop?motor_id=eq.$motorId&is_active=eq.true&select=id")
                if (!queryShop.isNullOrBlank()) {
                    val shopArr = JSONArray(queryShop)
                    if (shopArr.length() > 0) {
                        val shopId = shopArr.getJSONObject(0).optLong("id")
                        patch("shop?id=eq.$shopId", JSONObject().put("is_active", false).toString())
                    }
                }
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error returning scooter", e)
            return false
        }
    }

    fun saveScooter(scooter: Scooter): Boolean {
        return saveScooterAndGetId(scooter) != null
    }

    fun saveScooterAndGetId(scooter: Scooter): String? {
        if (!isConfigured()) return null
        try {
            val photoUrls = uploadImagesAndGetUrls("rentals", "scooter_${scooter.plateNumber.replace(" ", "_")}", scooter.images)
            
            // Find or create model
            var modelId: Long? = null
            val queryModel = get("model?brand=eq.${scooter.brand}&model=eq.${scooter.model}&select=id")
            if (!queryModel.isNullOrBlank()) {
                val arr = JSONArray(queryModel)
                if (arr.length() > 0) {
                    modelId = arr.getJSONObject(0).optLong("id")
                }
            }
            if (modelId == null) {
                val draft = ScooterModelDraft(
                    id = "",
                    brand = scooter.brand,
                    model = scooter.model,
                    cc = scooter.cc,
                    year = scooter.year,
                    hasPhoneCharger = scooter.hasPhoneCharger,
                    hasKeyless = scooter.hasKeyless,
                    hasAbs = scooter.hasAbs,
                    priceDaily = scooter.priceDaily,
                    price3Day = scooter.price3Day,
                    priceWeekly = scooter.priceWeekly,
                    price2Week = scooter.price2Week,
                    priceMonthly = scooter.priceMonthly,
                    depositAmount = scooter.depositAmount,
                    description = scooter.description,
                    fuelBars = scooter.fuelBars,
                    fuelCostPerBar = scooter.fuelCostPerBar
                )
                val modelIdStr = saveModelAndGetId(draft)
                modelId = modelIdStr?.toLongOrNull()
            }
            if (modelId == null) return null

            val motorJson = JSONObject().apply {
                put("model_id", modelId)
                put("plate_number", scooter.plateNumber)
                put("status", scooter.status.name)
                put("photo", photoUrls)
                put("fuel_bars", scooter.fuelBars)
                put("fuel_cost_per_bar", scooter.fuelCostPerBar)
            }.toString()
            val res = post("motor", motorJson) ?: return null
            val arr = JSONArray(res)
            if (arr.length() > 0) {
                return arr.getJSONObject(0).optLong("id").toString()
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error saving scooter", e)
            return null
        }
    }

    fun updateScooter(id: String, scooter: Scooter): Boolean {
        if (!isConfigured()) return false
        try {
            val photoUrls = uploadImagesAndGetUrls("rentals", "scooter_${scooter.plateNumber.replace(" ", "_")}", scooter.images)
            
            // Find or create model ID
            var modelId: Long? = null
            val queryModel = get("model?brand=eq.${scooter.brand}&model=eq.${scooter.model}&select=id")
            if (!queryModel.isNullOrBlank()) {
                val arr = JSONArray(queryModel)
                if (arr.length() > 0) {
                    modelId = arr.getJSONObject(0).optLong("id")
                }
            }
            if (modelId == null) {
                val draft = ScooterModelDraft(
                    id = "",
                    brand = scooter.brand,
                    model = scooter.model,
                    cc = scooter.cc,
                    year = scooter.year,
                    hasPhoneCharger = scooter.hasPhoneCharger,
                    hasKeyless = scooter.hasKeyless,
                    hasAbs = scooter.hasAbs,
                    priceDaily = scooter.priceDaily,
                    price3Day = scooter.price3Day,
                    priceWeekly = scooter.priceWeekly,
                    price2Week = scooter.price2Week,
                    priceMonthly = scooter.priceMonthly,
                    depositAmount = scooter.depositAmount,
                    description = scooter.description,
                    fuelBars = scooter.fuelBars,
                    fuelCostPerBar = scooter.fuelCostPerBar
                )
                val modelIdStr = saveModelAndGetId(draft)
                modelId = modelIdStr?.toLongOrNull()
            } else {
                // If model exists, update it with the scooter's specs!
                val draft = ScooterModelDraft(
                    id = modelId.toString(),
                    brand = scooter.brand,
                    model = scooter.model,
                    cc = scooter.cc,
                    year = scooter.year,
                    hasPhoneCharger = scooter.hasPhoneCharger,
                    hasKeyless = scooter.hasKeyless,
                    hasAbs = scooter.hasAbs,
                    priceDaily = scooter.priceDaily,
                    price3Day = scooter.price3Day,
                    priceWeekly = scooter.priceWeekly,
                    price2Week = scooter.price2Week,
                    priceMonthly = scooter.priceMonthly,
                    depositAmount = scooter.depositAmount,
                    description = scooter.description,
                    fuelBars = scooter.fuelBars,
                    fuelCostPerBar = scooter.fuelCostPerBar
                )
                updateModel(modelId.toString(), draft)
            }
            if (modelId == null) return false

            val motorJson = JSONObject().apply {
                put("model_id", modelId)
                put("plate_number", scooter.plateNumber)
                put("status", scooter.status.name)
                put("photo", photoUrls)
                put("fuel_bars", scooter.fuelBars)
                put("fuel_cost_per_bar", scooter.fuelCostPerBar)
            }.toString()
            val query = if (id.all { it.isDigit() }) "id=eq.$id" else "plate_number=eq.${scooter.plateNumber}"
            val res = patch("motor?$query", motorJson)
            return res != null
        } catch (e: Exception) {
            Log.e(TAG, "Error updating motor", e)
            return false
        }
    }

    fun pushMockData(onResult: (Boolean) -> Unit) {
        if (!isConfigured()) {
            onResult(false)
            return
        }
        
        kotlin.concurrent.thread {
            try {
                val query = get("motor?select=id&limit=1")
                if (query != null && query != "[]") {
                    onResult(true)
                    return@thread
                }
                
                // Pushing default fleet
                for (scooter in RentalRepository.scooters) {
                    saveScooter(scooter)
                }
                
                // Dummy customer & shop
                val custJson = JSONObject().apply {
                    put("name", "Somchai Somboon")
                    put("passport_id", "DL-987654")
                    put("license_id", "DL-987654")
                    put("address", "Phuket Hotel")
                    put("phone_number", "+66812345678")
                    put("email", "somchai@gmail.com")
                    put("whatsapp_number", "+66812345678")
                }.toString()
                val resCust = post("customer", custJson)
                
                if (resCust != null) {
                    val arr = JSONArray(resCust)
                    if (arr.length() > 0) {
                        val customerId = arr.getJSONObject(0).optLong("id")
                        val queryMotor = get("motor?limit=1&select=id")
                        if (queryMotor != null) {
                            val arrM = JSONArray(queryMotor)
                            if (arrM.length() > 0) {
                                val motorId = arrM.getJSONObject(0).optLong("id")
                                
                                val shopJson = JSONObject().apply {
                                    put("customer_id", customerId)
                                    put("motor_id", motorId)
                                    put("start_date", "2026-06-08")
                                    put("due_date", "2026-06-13")
                                    put("deposit", 3000.0)
                                    put("original_passport", false)
                                    put("helmet_count", 1)
                                }.toString()
                                post("shop", shopJson)
                            }
                        }
                    }
                }
                onResult(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing mock data", e)
                onResult(false)
            }
        }
    }

    fun fetchModels(): List<ScooterModelDraft>? {
        val jsonStr = get("model?select=*") ?: return null
        val list = mutableListOf<ScooterModelDraft>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val idStr = obj.optLong("id").toString()
                val brand = obj.optString("brand", "Honda")
                val model = obj.optString("model", "")
                val cc = obj.optInt("cc", 125)
                val year = obj.optInt("year", 2024)
                
                val featureStr = obj.optString("feature", "")
                val featuresList = featureStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val hasPhoneCharger = featuresList.any { it.contains("charger", ignoreCase = true) || it.contains("phone", ignoreCase = true) }
                val hasKeyless = featuresList.any { it.contains("keyless", ignoreCase = true) }
                val hasAbs = featuresList.any { it.contains("abs", ignoreCase = true) }

                val priceDaily = obj.optDouble("price_daily", 400.0)
                val price3Day = obj.optDouble("price_3day", priceDaily * 3.0 * 0.9)
                val priceWeekly = obj.optDouble("price_weekly", priceDaily * 7.0 * 0.85)
                val price2Week = obj.optDouble("price_2week", priceDaily * 14.0 * 0.75)
                val priceMonthly = obj.optDouble("price_monthly", priceDaily * 30.0 * 0.65)
                val depositAmount = obj.optDouble("deposit_amount", 0.0)
                val description = obj.optString("description", "")
                val fuelBars = obj.optInt("fuel_bars", 6)
                val fuelCostPerBar = obj.optDouble("fuel_cost_per_bar", 100.0)

                val draft = ScooterModelDraft(
                    id = idStr,
                    brand = brand,
                    model = model,
                    cc = cc,
                    year = year,
                    hasPhoneCharger = hasPhoneCharger,
                    hasKeyless = hasKeyless,
                    hasAbs = hasAbs,
                    priceDaily = priceDaily,
                    price3Day = price3Day,
                    priceWeekly = priceWeekly,
                    price2Week = price2Week,
                    priceMonthly = priceMonthly,
                    depositAmount = depositAmount,
                    description = description,
                    fuelBars = fuelBars,
                    fuelCostPerBar = fuelCostPerBar
                )
                list.add(draft)
            }
            return list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing models", e)
            return null
        }
    }

    fun saveModel(draft: ScooterModelDraft): Boolean {
        return saveModelAndGetId(draft) != null
    }

    fun saveModelAndGetId(draft: ScooterModelDraft): String? {
        if (!isConfigured()) return null
        try {
            val featuresToSave = mutableListOf<String>()
            if (draft.hasPhoneCharger) featuresToSave.add("Phone Charger")
            if (draft.hasKeyless) featuresToSave.add("Keyless")
            if (draft.hasAbs) featuresToSave.add("ABS")
            val featureStr = featuresToSave.joinToString(",")

            val modelJson = JSONObject().apply {
                put("brand", draft.brand)
                put("model", draft.model)
                put("cc", draft.cc)
                put("year", draft.year)
                put("price_daily", draft.priceDaily)
                put("price_3day", draft.price3Day)
                put("price_weekly", draft.priceWeekly)
                put("price_2week", draft.price2Week)
                put("price_monthly", draft.priceMonthly)
                put("deposit_amount", draft.depositAmount)
                put("description", draft.description)
                put("fuel_bars", draft.fuelBars)
                put("fuel_cost_per_bar", draft.fuelCostPerBar)
                put("feature", featureStr)
            }.toString()
            val res = post("model", modelJson) ?: return null
            val arr = JSONArray(res)
            if (arr.length() > 0) {
                val newId = arr.getJSONObject(0).optLong("id")
                return newId.toString()
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error saving model", e)
            return null
        }
    }

    fun updateModel(id: String, draft: ScooterModelDraft): Boolean {
        if (!isConfigured()) return false
        try {
            val featuresToSave = mutableListOf<String>()
            if (draft.hasPhoneCharger) featuresToSave.add("Phone Charger")
            if (draft.hasKeyless) featuresToSave.add("Keyless")
            if (draft.hasAbs) featuresToSave.add("ABS")
            val featureStr = featuresToSave.joinToString(",")

            val modelJson = JSONObject().apply {
                put("brand", draft.brand)
                put("model", draft.model)
                put("cc", draft.cc)
                put("year", draft.year)
                put("price_daily", draft.priceDaily)
                put("price_3day", draft.price3Day)
                put("price_weekly", draft.priceWeekly)
                put("price_2week", draft.price2Week)
                put("price_monthly", draft.priceMonthly)
                put("deposit_amount", draft.depositAmount)
                put("description", draft.description)
                put("fuel_bars", draft.fuelBars)
                put("fuel_cost_per_bar", draft.fuelCostPerBar)
                put("feature", featureStr)
            }.toString()
            val query = if (id.all { it.isDigit() }) "id=eq.$id" else "model=eq.${draft.model}"
            val res = patch("model?$query", modelJson)
            return res != null
        } catch (e: Exception) {
            Log.e(TAG, "Error updating model", e)
            return false
        }
    }

    fun deleteModel(id: String): Boolean {
        if (!isConfigured()) return false
        return try {
            val query = "id=eq.$id"
            val res = delete("model?$query")
            res != null
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model", e)
            false
        }
    }
}
