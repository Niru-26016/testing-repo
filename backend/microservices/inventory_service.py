import os
import time
import requests
import traceback
from typing import Dict, Any, List
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

app = FastAPI(title="Inventory Microservice", version="1.0.0")

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
    ARGUS Production Observer Middleware for Inventory Microservice:
    Intercepts unhandled runtime exceptions, formats stack trace,
    and posts crash payload to ARGUS /webhook/crash.
    """
    try:
        response = await call_next(request)
        return response
    except Exception as exc:
        raw_stack = traceback.format_exc()
        error_msg = f"{type(exc).__name__}: {str(exc)}"
        trace_id = f"tr-inventory-{int(time.time())}"
        
        payload = {
            "trace_id": trace_id,
            "error_message": error_msg,
            "stack_trace": raw_stack,
            "commit_sha": CURRENT_GIT_COMMIT
        }
        
        try:
            requests.post(ARGUS_WEBHOOK_URL, json=payload, timeout=2.0)
            print(f"[ARGUS Inventory Observer] Dispatched crash webhook for {trace_id}: {error_msg}")
        except Exception as net_err:
            print(f"[ARGUS Inventory Observer] Webhook offline: {net_err}")
            
        return JSONResponse(
            status_code=500,
            content={
                "error": "500 Internal Server Error in Inventory Microservice",
                "message": f"Runtime exception in inventory service: {error_msg}",
                "trace_id": trace_id,
                "argus_notified": True
            }
        )


class InventoryRequest(BaseModel):
    items: List[Dict[str, Any]]


def verify_inventory_logic(cart_items: List[Dict[str, Any]]) -> Dict[str, Any]:
    in_stock_items = [item for item in cart_items if item.get("quantity", 0) > 0]
    
    # BUG LOCATION: Unchecked array access in_stock_items[0] when list is empty
    # Throws IndexError: list index out of range
    lead_item = in_stock_items[0]
    
    return {
        "status": "AVAILABLE",
        "primary_item_sku": lead_item.get("id", "SKU-UNKNOWN"),
        "total_units": sum(item.get("quantity", 0) for item in in_stock_items)
    }


@app.get("/inventory/health")
def health():
    return {"status": "ONLINE", "service": "Inventory Microservice", "commit": CURRENT_GIT_COMMIT}


@app.post("/inventory/verify")
def verify_inventory(req: InventoryRequest):
    return verify_inventory_logic(req.items)
