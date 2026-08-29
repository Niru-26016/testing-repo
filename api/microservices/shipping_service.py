import os
import time
import requests
import traceback
from typing import Dict, Any
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

app = FastAPI(title="Shipping Microservice", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ARGUS_WEBHOOK_URL = os.getenv("ARGUS_WEBHOOK_URL", "http://127.0.0.1:8000/webhook/crash")
CURRENT_GIT_COMMIT = os.getenv("VERCEL_GIT_COMMIT_SHA", "8a3d12f")


@app.middleware("http")
async def argus_observer_middleware(request: Request, call_next):
    """
    ARGUS Production Observer Middleware for Shipping Microservice:
    Intercepts unhandled runtime exceptions, formats stack trace,
    and posts crash payload to ARGUS /webhook/crash.
    """
    try:
        response = await call_next(request)
        return response
    except Exception as exc:
        raw_stack = traceback.format_exc()
        error_msg = f"{type(exc).__name__}: {str(exc)}"
        trace_id = f"tr-shipping-{int(time.time())}"
        
        payload = {
            "trace_id": trace_id,
            "error_message": error_msg,
            "stack_trace": raw_stack,
            "commit_sha": CURRENT_GIT_COMMIT
        }
        
        try:
            requests.post(ARGUS_WEBHOOK_URL, json=payload, timeout=2.0)
            print(f"[ARGUS Shipping Observer] Dispatched crash webhook for {trace_id}: {error_msg}")
        except Exception as net_err:
            print(f"[ARGUS Shipping Observer] Webhook offline: {net_err}")
            
        return JSONResponse(
            status_code=500,
            content={
                "error": "500 Internal Server Error in Shipping Microservice",
                "message": f"Runtime exception in shipping service: {error_msg}",
                "trace_id": trace_id,
                "argus_notified": True
            }
        )


class ShippingRequest(BaseModel):
    address: Dict[str, Any]
    weight_kg: float = 1.0


def calculate_shipping_logic(address: Dict[str, Any], weight_kg: float = 1.0) -> Dict[str, Any]:
    country = address.get("country", "US").upper()
    
    if country == "US":
        # BUG LOCATION: Direct key lookup without dict.get() safety guard
        # Throws KeyError when missing 'zip_code' in address dict
        postal_code = address["zip_code"]
        prefix = postal_code[:3] if len(postal_code) >= 3 else "000"
        
        rate = 5.99 + (weight_kg * 1.5)
        if prefix.startswith("90"):
            rate += 2.50
            
        return {
            "carrier": "USPS Priority Mail",
            "estimated_days": 2,
            "shipping_fee": round(rate, 2)
        }
    else:
        return {
            "carrier": "DHL Express International",
            "estimated_days": 5,
            "shipping_fee": round(19.99 + (weight_kg * 3.0), 2)
        }


@app.get("/shipping/health")
def health():
    return {"status": "ONLINE", "service": "Shipping Microservice", "commit": CURRENT_GIT_COMMIT}


@app.post("/shipping/rates")
def calculate_shipping(req: ShippingRequest):
    return calculate_shipping_logic(req.address, req.weight_kg)
