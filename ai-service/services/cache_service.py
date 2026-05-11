import redis
import hashlib
import json
import os

class CacheService:

    def __init__(self):
        # In Docker, Redis is reachable via the service name 'redis', not 'localhost'
        redis_host = os.getenv("REDIS_HOST", "localhost")
        redis_port = int(os.getenv("REDIS_PORT", "6379"))
        self.client = redis.Redis(host=redis_host, port=redis_port, decode_responses=True)
        self.ttl = 900

        self.hits = 0
        self.misses = 0

    def _key(self, text):
        return hashlib.sha256(text.encode()).hexdigest()

    def get(self, text):
        key = self._key(text)
        data = self.client.get(key)

        if data:
            self.hits += 1
            return json.loads(data)

        self.misses += 1
        return None

    def set(self, text, value):
        key = self._key(text)
        self.client.setex(key, self.ttl, json.dumps(value))