FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend application code
COPY . .

# Expose API Gateway and microservice ports
EXPOSE 8001 8002 8003 8004

ENV PYTHONUNBUFFERED=1
ENV HOST=0.0.0.0

CMD ["python", "run_microservices.py"]
