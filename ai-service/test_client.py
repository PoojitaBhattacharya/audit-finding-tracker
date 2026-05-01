from services.groq_client import GroqClient

client = GroqClient()

response = client.generate("Explain audit risk in one sentence")

print(response)