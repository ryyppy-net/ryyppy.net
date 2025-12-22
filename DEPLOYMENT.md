# Deployment Setup with Traefik

Production deployment configuration using Traefik as a reverse proxy. Currently configured with a whoami service to verify routing - this will be replaced with the actual ryyppy.net application.

## What's Included

- **Traefik**: Reverse proxy listening on ports 80, 443, and 8080 (dashboard)
- **Whoami**: Placeholder service to verify routing works (will be replaced with ryyppy.net app)

## How to Run

### 1. Start the services

```bash
docker compose up -d
```

### 2. Check that containers are running

```bash
docker ps
```

You should see two containers: `traefik` and `whoami`

### 3. Test the routing

**From your browser:**
```
http://YOUR_VPS_IP
```

**From within the VPS (SSH):**
```bash
curl http://localhost
```

You should see output like:
```
Hostname: abc123
IP: 172.x.x.x
RemoteAddr: 172.x.x.x:xxxxx
GET / HTTP/1.1
Host: YOUR_VPS_IP
...
```

### 4. View Traefik Dashboard

Open in browser:
```
http://YOUR_VPS_IP:8080
```

Replace `YOUR_VPS_IP` with your actual Hetzner server IP address.

The dashboard shows:
- All active routers
- Services being proxied
- Entrypoints (ports)
- Health status

### 5. Understanding the Labels

In `docker-compose.yml`, the whoami service has these labels:

```yaml
- "traefik.enable=true"
  # Tell Traefik to proxy this container

- "traefik.http.routers.whoami.rule=PathPrefix(`/`)"
  # Route ALL requests (any host) to this service
  # Before DNS is configured - will change to Host(`ryyppy.net`) later

- "traefik.http.routers.whoami.entrypoints=web"
  # Use the "web" entrypoint (port 80)

- "traefik.http.services.whoami.loadbalancer.server.port=80"
  # The container listens on port 80
```

### 6. Stop the services

```bash
docker compose down
```

## How Traffic Flows

1. Request comes to VPS on port 80
2. Traefik receives it
3. Traefik matches the request against routing rules
4. Routes to the appropriate container (currently whoami)
5. Container responds
6. Traefik sends response back to client

## Next Steps

1. ✅ Verify Traefik routing works (current step)
2. ⬜ Configure DNS to point ryyppy.net to the VPS
3. ⬜ Add SSL/TLS with Let's Encrypt
4. ⬜ Replace whoami with ryyppy.net application
5. ⬜ Update GitHub Actions to deploy on build
6. ⬜ Update routing rule to Host(`ryyppy.net`)

## Troubleshooting

**Nothing works?**
```bash
# Check logs
docker compose logs traefik
docker compose logs whoami

# Check if ports are in use
sudo netstat -tlnp | grep :80
sudo netstat -tlnp | grep :8080
```

**Port 80 already in use?**
If you have nginx or another web server running, stop it first:
```bash
sudo systemctl stop nginx
# or
sudo systemctl stop apache2
```
