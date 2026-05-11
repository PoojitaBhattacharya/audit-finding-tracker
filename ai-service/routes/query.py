from flask import Blueprint, request, jsonify
from services.groq_client import GroqClient
from services.chroma_client import ChromaClient

bp = Blueprint("query", __name__)

# Lazy-init: avoids crashing the whole app at import if GroqClient fails
_groq = None
_chroma = None

def get_groq():
    global _groq
    if _groq is None:
        _groq = GroqClient()
    return _groq

def get_chroma():
    global _chroma
    if _chroma is None:
        _chroma = ChromaClient()
        # Seed with baseline audit knowledge docs
        _chroma.add_documents([
            "SQL injection is a major security vulnerability",
            "Weak authentication leads to data breaches",
            "Poor input validation causes security risks",
            "Encryption is required for sensitive data"
        ])
    return _chroma

def load_prompt(context, question):
    # Path is relative to WORKDIR /app inside the container
    with open("prompts/query_prompt.txt", "r") as f:
        return f.read().replace("{context}", context).replace("{question}", question)

@bp.route("/query", methods=["POST"])
def query():
    data = request.get_json()

    if not data or "question" not in data:
        return jsonify({"error": "Missing 'question' field"}), 400

    question = data["question"]
    groq = get_groq()
    chroma = get_chroma()

    docs = chroma.query(question)
    context = "\n".join(docs[0]) if docs else ""

    prompt = load_prompt(context, question)
    result = groq.generate(prompt)

    return jsonify({
        "answer": result["output"],
        "sources": docs,
        "meta": {
            "tokens_used": result["tokens_used"],
            "response_time_ms": result["response_time_ms"],
            "model": result["model"],
            "fallback": result["fallback"]
        }
    })