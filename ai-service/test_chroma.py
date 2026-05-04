from services.chroma_client import ChromaClient

client = ChromaClient()

docs = [
    "SQL injection vulnerability detected in login",
    "Slow performance in dashboard loading",
    "UI issue on mobile view alignment",
    "Compliance issue with data storage policy"
]

client.add_documents(docs)

results = client.query("database security problem")

print(results)