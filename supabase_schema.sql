-- Consolidated Supabase Schema for CheapAsChip Scooter Rental App
-- Copy and paste this directly into the Supabase SQL Editor

-- 1. Create model table
CREATE TABLE IF NOT EXISTS model (
  id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  brand text NOT NULL,
  model text NOT NULL,
  cc int NOT NULL,
  year int NOT NULL,
  price_daily numeric(10, 2) NOT NULL DEFAULT 0.00,
  price_3day numeric(10, 2) NOT NULL DEFAULT 0.00,
  price_weekly numeric(10, 2) NOT NULL DEFAULT 0.00,
  price_2week numeric(10, 2) NOT NULL DEFAULT 0.00,
  price_monthly numeric(10, 2) NOT NULL DEFAULT 0.00,
  deposit_amount numeric(10, 2) NOT NULL DEFAULT 0.00,
  description text DEFAULT '',
  fuel_bars int NOT NULL DEFAULT 6,
  fuel_cost_per_bar numeric(10, 2) NOT NULL DEFAULT 100.00,
  feature text DEFAULT ''
);

-- 3. Create motor table
CREATE TABLE IF NOT EXISTS motor (
  id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  model_id bigint REFERENCES model (id) ON DELETE CASCADE NOT NULL,
  photo text,
  plate_number text UNIQUE NOT NULL,
  status text,
  fuel_bars int NOT NULL DEFAULT 6,
  fuel_cost_per_bar numeric(10, 2) NOT NULL DEFAULT 100.00
);

-- 4. Create customer table
CREATE TABLE IF NOT EXISTS customer (
  id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  name text NOT NULL,
  passport_id text UNIQUE NOT NULL,
  license_id text UNIQUE NOT NULL,
  address text,
  phone_number text,
  email text,
  whatsapp_number text,
  passport_photo text,
  license_photo text,
  CONSTRAINT contact_not_all_null CHECK (
    phone_number IS NOT NULL
    OR email IS NOT NULL
    OR whatsapp_number IS NOT NULL
  )
);

-- 5. Create shop table
CREATE TABLE IF NOT EXISTS shop (
  id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  customer_id bigint REFERENCES customer (id) ON DELETE CASCADE NOT NULL,
  motor_id bigint REFERENCES motor (id) ON DELETE CASCADE NOT NULL,
  start_date date NOT NULL,
  due_date date NOT NULL,
  deposit numeric(10, 2) NOT NULL,
  original_passport boolean DEFAULT false,
  helmet_count int NOT NULL DEFAULT 1 CHECK (helmet_count >= 1),
  is_active boolean DEFAULT true,
  total_price numeric(10, 2) NOT NULL DEFAULT 0.00,
  scratch_photos text
);

-- Enable Row Level Security (RLS)
ALTER TABLE model ENABLE ROW LEVEL SECURITY;
ALTER TABLE motor ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop ENABLE ROW LEVEL SECURITY;

-- Allow public anonymous read/write/update/delete access for development testing
DROP POLICY IF EXISTS "Allow public read access" ON model;
DROP POLICY IF EXISTS "Allow public insert access" ON model;
DROP POLICY IF EXISTS "Allow public update access" ON model;
DROP POLICY IF EXISTS "Allow public delete access" ON model;
CREATE POLICY "Allow public read access" ON model FOR SELECT USING (true);
CREATE POLICY "Allow public insert access" ON model FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access" ON model FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access" ON model FOR DELETE USING (true);

DROP POLICY IF EXISTS "Allow public read access" ON motor;
DROP POLICY IF EXISTS "Allow public insert access" ON motor;
DROP POLICY IF EXISTS "Allow public update access" ON motor;
DROP POLICY IF EXISTS "Allow public delete access" ON motor;
CREATE POLICY "Allow public read access" ON motor FOR SELECT USING (true);
CREATE POLICY "Allow public insert access" ON motor FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access" ON motor FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access" ON motor FOR DELETE USING (true);

DROP POLICY IF EXISTS "Allow public read access" ON customer;
DROP POLICY IF EXISTS "Allow public insert access" ON customer;
DROP POLICY IF EXISTS "Allow public update access" ON customer;
DROP POLICY IF EXISTS "Allow public delete access" ON customer;
CREATE POLICY "Allow public read access" ON customer FOR SELECT USING (true);
CREATE POLICY "Allow public insert access" ON customer FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access" ON customer FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access" ON customer FOR DELETE USING (true);

DROP POLICY IF EXISTS "Allow public read access" ON shop;
DROP POLICY IF EXISTS "Allow public insert access" ON shop;
DROP POLICY IF EXISTS "Allow public update access" ON shop;
DROP POLICY IF EXISTS "Allow public delete access" ON shop;
CREATE POLICY "Allow public read access" ON shop FOR SELECT USING (true);
CREATE POLICY "Allow public insert access" ON shop FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update access" ON shop FOR UPDATE USING (true);
CREATE POLICY "Allow public delete access" ON shop FOR DELETE USING (true);

-- Create storage bucket for photos and configure public RLS access policies
INSERT INTO storage.buckets (id, name, public)
VALUES ('rentals', 'rentals', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Allow public select" ON storage.objects;
DROP POLICY IF EXISTS "Allow public insert" ON storage.objects;

CREATE POLICY "Allow public select" ON storage.objects
  FOR SELECT USING (bucket_id = 'rentals');

CREATE POLICY "Allow public insert" ON storage.objects
  FOR INSERT WITH CHECK (bucket_id = 'rentals');
