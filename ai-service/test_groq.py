import requests
import os

API_KEY = os.getenv("GROQ_API_KEY")

url = "https://api.groq.com/openai/v1/chat/completions"

headers = {
    "Authorization": f"Bearer {API_KEY}",
    "Content-Type": "application/json"
}

data = {
    "model": "llama-3.3-70b-versatile",
    "messages": [
        {"role": "user", "content": "Explain what an audit finding is in 2 lines"}
    ],
    "temperature": 0.3
}

response = requests.post(url, headers=headers, json=data)

print(response.json())