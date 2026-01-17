-- Ramble Database Schema
-- Run this in your Supabase SQL Editor to set up the database

-- Enable UUID extension (usually already enabled)
create extension if not exists "uuid-ossp";

-- ============================================
-- TABLES
-- ============================================

-- User profiles (extends Supabase auth.users)
create table if not exists public.profiles (
  id uuid references auth.users on delete cascade primary key,
  email text not null,
  created_at timestamptz default now() not null
);

-- Subscriptions (for Google Play Billing)
create table if not exists public.subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references public.profiles(id) on delete cascade not null,
  status text not null default 'inactive', -- 'active', 'inactive', 'cancelled', 'expired'
  provider text, -- 'google_play', 'stripe', etc.
  external_id text, -- purchase token or subscription ID
  expires_at timestamptz,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

-- Voucher codes (created by admin)
create table if not exists public.vouchers (
  id uuid primary key default gen_random_uuid(),
  code text unique not null,
  description text,
  max_redemptions int default 1 not null,
  current_redemptions int default 0 not null,
  is_active boolean default true not null,
  created_at timestamptz default now() not null
);

-- Voucher redemptions (grants permanent access)
create table if not exists public.voucher_redemptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references public.profiles(id) on delete cascade not null,
  voucher_id uuid references public.vouchers(id) on delete cascade not null,
  redeemed_at timestamptz default now() not null,
  unique(user_id, voucher_id)
);

-- Usage tracking
create table if not exists public.usage_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references public.profiles(id) on delete cascade not null,
  session_id text,
  duration_seconds int not null default 0,
  created_at timestamptz default now() not null
);

-- ============================================
-- INDEXES
-- ============================================

create index if not exists idx_subscriptions_user_id on public.subscriptions(user_id);
create index if not exists idx_subscriptions_status on public.subscriptions(status);
create index if not exists idx_voucher_redemptions_user_id on public.voucher_redemptions(user_id);
create index if not exists idx_usage_logs_user_id on public.usage_logs(user_id);
create index if not exists idx_usage_logs_created_at on public.usage_logs(created_at);

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to handle new user signup
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email)
  values (new.id, new.email);
  return new;
end;
$$ language plpgsql security definer;

-- Trigger to create profile on signup
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ============================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================

-- Enable RLS on all tables
alter table public.profiles enable row level security;
alter table public.subscriptions enable row level security;
alter table public.vouchers enable row level security;
alter table public.voucher_redemptions enable row level security;
alter table public.usage_logs enable row level security;

-- Profiles: Users can read their own profile
create policy "Users can view own profile"
  on public.profiles for select
  using (auth.uid() = id);

-- Subscriptions: Users can read their own subscriptions
create policy "Users can view own subscriptions"
  on public.subscriptions for select
  using (auth.uid() = user_id);

-- Vouchers: Anyone can read active vouchers (for validation)
-- Note: The actual redemption check is done server-side with service role
create policy "Anyone can view active vouchers"
  on public.vouchers for select
  using (is_active = true);

-- Voucher Redemptions: Users can read their own redemptions
create policy "Users can view own redemptions"
  on public.voucher_redemptions for select
  using (auth.uid() = user_id);

-- Usage Logs: Users can read their own logs
create policy "Users can view own usage logs"
  on public.usage_logs for select
  using (auth.uid() = user_id);

-- ============================================
-- GRANTS
-- ============================================

-- Grant access to authenticated users
grant usage on schema public to authenticated;
grant select on public.profiles to authenticated;
grant select on public.subscriptions to authenticated;
grant select on public.vouchers to authenticated;
grant select on public.voucher_redemptions to authenticated;
grant select on public.usage_logs to authenticated;

-- Grant access to service role (for admin operations)
grant all on public.profiles to service_role;
grant all on public.subscriptions to service_role;
grant all on public.vouchers to service_role;
grant all on public.voucher_redemptions to service_role;
grant all on public.usage_logs to service_role;
