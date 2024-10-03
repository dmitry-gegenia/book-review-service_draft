SELECT 'CREATE DATABASE book_service'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'book_service')\gexec


