from typing import List, Dict, Any

def verify_inventory_availability(cart_items: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Verifies stock availability for cart items before checkout.
    BUG: Unchecked array index access items[0] when filtered selection is empty.
    """
    in_stock_items = [item for item in cart_items if item.get("quantity", 0) > 0]
    
    # BUG LOCATION: Accessing first element without checking if list is non-empty
    lead_item = in_stock_items[0]  # <--- IndexError: list index out of range if empty list
    
    return {
        "status": "AVAILABLE",
        "primary_item_sku": lead_item.get("id", "SKU-UNKNOWN"),
        "total_units": sum(item.get("quantity", 0) for item in in_stock_items)
    }
