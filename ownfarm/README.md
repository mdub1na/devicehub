1. Run nginx only  
````
docker compose -f docker-compose-nginx-only.yaml up -d
````
2. Run letsencrypt to generate cert  
````
docker compose -f docker-compose-certbot.yaml run --rm certbot certonly \  
    --webroot \  
    -w /var/www/certbot \  
    -d devicehub.putmyhexon.ru \  
    -d ldap.putmyhexon.ru \  
    -d appium-grid.putmyhexon.ru \  
    --email ttork354@yandex.ru \  
    --agree-tos \  
    --no-eff-email \  
    --verbose
````
3. Run farm
````
docker compose -f docker-compose-prod-letsencrypt.yaml --env-file scripts/variables.env up -d
````
4. Down farm
````
docker compose -f docker-compose-prod-letsencrypt.yaml --env-file scripts/variables.env down
````
Build image from repo  
````
docker build --no-cache -t vkcom/devicehub:latest .
````