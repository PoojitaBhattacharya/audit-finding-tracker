import requests
import os
import time

class GroqClient:

    def __init__(self):
        self.api_key = os.getenv("GROQ_API_KEY")

        if not self.api_key:
            raise ValueError("GROQ_API_KEY not found in environment variables")

        self.url = "https://api.groq.com/openai/v1/chat/completions"

        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

        self.model = "llama-3.3-70b-versatile"

        self.response_times = []

    def generate(self, prompt, retries=3):
        for attempt in range(retries):
            try:
                start_time = time.time()

                response = requests.post(
                    self.url,
                    headers=self.headers,
                    json={
                        "model": self.model,
                        "messages": [
                            {"role": "user", "content": prompt}
                        ],
                        "temperature": 0.3
                    },
                    timeout=10
                )

                if response.status_code != 200:
                    raise Exception(f"API Error: {response.text}")

                result = response.json()

                output = result["choices"][0]["message"]["content"]
                tokens = result.get("usage", {}).get("total_tokens", 0)

                response_time = int((time.time() - start_time) * 1000)

                self.response_times.append(response_time)
                
                if len(self.response_times) > 10:
                    self.response_times.pop(0)

                return {
                    "output": output,
                    "tokens_used": tokens,
                    "response_time_ms": response_time,
                    "model": self.model,
                    "fallback": False
                }

            except Exception as e:
                print(f"[Retry {attempt+1}] Error:", e)
                time.sleep(2)

        return {
            "output": "AI service temporarily unavailable. Showing default response.",
            "tokens_used": 0,
            "response_time_ms": 0,
            "model": self.model,
            "fallback": True
        }