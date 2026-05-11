from flask import Blueprint, g, jsonify
from datetime import datetime, timezone

describe_bp = Blueprint("describe", __name__)

@describe_bp.route("/describe", methods=["POST"])
def describe():
    data = g.sanitized_data
    text = data.get("text", "")

    # Placeholder: Load prompt from prompts/describe_prompt.txt
    # with open("prompts/describe_prompt.txt", "r") as f:
    #     prompt_template = f.read()

    # Placeholder: Call GroqClient
    # client = GroqClient()
    # response = client.generate(prompt_template.replace("{input}", text))
    
    simulated_output = f"Simulated LLM description for: {text[:50]}..."

    return jsonify({
        "output": simulated_output,
        "generated_at": datetime.now(timezone.utc).isoformat()
    })
