-- Supabase Storage Row-Level Security Rules for AdoptUs

-- 1. Pastikan bucket 'adoptus-post-images' dibuat dan diatur public
insert into storage.buckets (id, name, public)
values ('adoptus-post-images', 'adoptus-post-images', true)
on conflict (id) do nothing;

-- 2. Hapus policy lama jika ada untuk menghindari konflik
drop policy if exists "Allow public read access to adoptus-post-images" on storage.objects;
drop policy if exists "Allow authenticated upload to own folder" on storage.objects;
drop policy if exists "Allow authenticated delete from own folder" on storage.objects;

-- 3. Policy untuk mengizinkan siapa saja membaca media di bucket 'adoptus-post-images'
create policy "Allow public read access to adoptus-post-images"
on storage.objects for select
using ( bucket_id = 'adoptus-post-images' );

-- 4. Policy untuk mengizinkan user terautentikasi mengunggah ke folder milik sendiri (posts/{uid}/*)
create policy "Allow authenticated upload to own folder"
on storage.objects for insert
with check (
  bucket_id = 'adoptus-post-images'
  and auth.role() = 'authenticated'
  and (storage.foldername(name))[1] = 'posts'
  and (storage.foldername(name))[2] = auth.uid()::text
);

-- 5. Policy untuk mengizinkan user terautentikasi menghapus media dari folder milik sendiri (posts/{uid}/*)
create policy "Allow authenticated delete from own folder"
on storage.objects for delete
using (
  bucket_id = 'adoptus-post-images'
  and auth.role() = 'authenticated'
  and (storage.foldername(name))[1] = 'posts'
  and (storage.foldername(name))[2] = auth.uid()::text
);
