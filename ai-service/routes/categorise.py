from flask import Blueprint, request, jsonify
from services.groq_client import GroqClient
import json

bp = Blueprint("categorise", __name__)
client = GroqClient()

def load_prompt(text):
    with open("ai-service/prompts/categorise_prompt.txt", "r") as f:
        return f.read().replace("{input}", text)

@bp.route("/categorise", methods=["POST"])
def categorise():
    data = request.get_json()

    if not data or "text" not in data:
        return jsonify({"error": "Missing 'text' field"}), 400

    prompt = load_prompt(data["text"])
    result = client.generate(prompt)

    try:
        parsed = json.loads(result["output"])
    except:
        parsed = {
            "category": "Unknown",
            "confidence": 0.0,
            "reasoning": result["output"]
        }

    return jsonify({
        "data": parsed,
        "meta": {
            "tokens_used": result["tokens_used"],
            "response_time_ms": result["response_time_ms"],
            "model": result["model"],
            "fallback": result["fallback"]
        }
    })