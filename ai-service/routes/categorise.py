from flask import Blueprint, request, jsonify
from services.groq_client import GroqClient
from services.cache_service import CacheService
import json
import uuid
from datetime import datetime

bp = Blueprint("categorise", __name__)

# Lazy-init: avoids crashing at import if GROQ_API_KEY is not yet set
_client = None
_cache = None

def get_client():
    global _client
    if _client is None:
        _client = GroqClient()
    return _client

def get_cache():
    global _cache
    if _cache is None:
        _cache = CacheService()
    return _cache

def load_prompt(text):
    # Path is relative to WORKDIR /app inside the container
    with open("prompts/categorise_prompt.txt", "r") as f:
        return f.read().replace("{input}", text)


@bp.route("/categorise", methods=["POST"])
def categorise():
    data = request.get_json()

    if not data or "text" not in data:
        return jsonify({"error": "Missing 'text' field"}), 400

    text = data["text"]
    client = get_client()
    cache = get_cache()

    cached = cache.get(text)
    if cached:
        return jsonify({
            "request_id": str(uuid.uuid4()),
            "timestamp": datetime.utcnow().isoformat(),
            
            "data": cached,
            "meta": {
                "cached": True
            }
        })

    prompt = load_prompt(text)
    result = client.generate(prompt)

    try:
        parsed = json.loads(result["output"])
    except:
        parsed = {
            "category": "Unknown",
            "confidence": 0.0,
            "reasoning": result["output"]
        }

    if parsed:
        cache.set(text, parsed)

    return jsonify({
    "request_id": str(uuid.uuid4()),
    "timestamp": datetime.utcnow().isoformat(),

    "data": parsed,   # ✅ FIXED (not cached)

    "meta": {
        "cached": False,
        "tokens_used": result["tokens_used"],
        "response_time_ms": result["response_time_ms"],
        "model": result["model"],
        "fallback": result["fallback"]
    }
})