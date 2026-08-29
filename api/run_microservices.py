import uvicorn
import multiprocessing

def run_gateway():
    print("[Microservices Runner] Starting API Gateway on http://127.0.0.1:8001")
    uvicorn.run("api.index:app", host="127.0.0.1", port=8001, reload=True)

def run_pricing():
    print("[Microservices Runner] Starting Pricing Microservice on http://127.0.0.1:8002")
    uvicorn.run("api.microservices.pricing_service:app", host="127.0.0.1", port=8002, reload=True)

def run_shipping():
    print("[Microservices Runner] Starting Shipping Microservice on http://127.0.0.1:8003")
    uvicorn.run("api.microservices.shipping_service:app", host="127.0.0.1", port=8003, reload=True)

def run_inventory():
    print("[Microservices Runner] Starting Inventory Microservice on http://127.0.0.1:8004")
    uvicorn.run("api.microservices.inventory_service:app", host="127.0.0.1", port=8004, reload=True)

if __name__ == "__main__":
    processes = [
        multiprocessing.Process(target=run_gateway),
        multiprocessing.Process(target=run_pricing),
        multiprocessing.Process(target=run_shipping),
        multiprocessing.Process(target=run_inventory)
    ]
    for p in processes:
        p.start()
    for p in processes:
        p.join()
