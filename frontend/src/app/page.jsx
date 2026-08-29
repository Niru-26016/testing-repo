'use client';

import React, { useState } from 'react';
import { 
  ShoppingBag, 
  CheckCircle2, 
  Tag, 
  ArrowRight, 
  RefreshCw, 
  AlertTriangle,
  Search,
  Star,
  Sparkles,
  ShieldAlert,
  X
} from 'lucide-react';

const PRODUCTS = [
  {
    id: "prod_1",
    name: "Developer Fleece Hoodie",
    category: "Apparel",
    price: 65.00,
    rating: 4.9,
    reviewsCount: 128,
    image: "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500&auto=format&fit=crop&q=60",
    description: "Premium heavyweight fleece hoodie engineered for long coding sessions."
  },
  {
    id: "prod_2",
    name: "Mechanical Coding Keyboard",
    category: "Hardware",
    price: 120.00,
    rating: 4.8,
    reviewsCount: 94,
    image: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop&q=60",
    description: "Hot-swappable tactile mechanical keyboard designed for fast pair programming."
  },
  {
    id: "prod_3",
    name: "UltraWide 4K Developer Monitor",
    category: "Electronics",
    price: 450.00,
    rating: 4.9,
    reviewsCount: 210,
    image: "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500&auto=format&fit=crop&q=60",
    description: "34-inch curved monitor with dual-pane layout for IDE code inspection and logs."
  },
  {
    id: "prod_4",
    name: "Ceramic Developer Coffee Mug",
    category: "Accessories",
    price: 22.00,
    rating: 4.7,
    reviewsCount: 56,
    image: "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500&auto=format&fit=crop&q=60",
    description: "Matte ceramic mug designed to keep your coffee warm through build iterations."
  }
];

export default function StorefrontPage() {
  const [cart, setCart] = useState([
    { ...PRODUCTS[0], quantity: 1 }
  ]);
  const [promoCode, setPromoCode] = useState("");
  const [address, setAddress] = useState({ country: "US", zip_code: "90210" });
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [checkoutResult, setCheckoutResult] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");

  const addToCart = (product) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
        );
      }
      return [...prev, { ...product, quantity: 1 }];
    });
  };

  const removeFromCart = (id) => {
    setCart((prev) => prev.filter((item) => item.id !== id));
  };

  const updateQuantity = (id, delta) => {
    setCart((prev) =>
      prev
        .map((item) => {
          if (item.id === id) {
            const newQty = item.quantity + delta;
            return newQty > 0 ? { ...item, quantity: newQty } : null;
          }
          return item;
        })
        .filter(Boolean)
    );
  };

  const cartSubtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const handleCheckout = async () => {
    setLoading(true);
    setCheckoutResult(null);

    try {
      const res = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          items: cart.map((i) => ({ id: i.id, price: i.price, quantity: i.quantity })),
          promo_code: promoCode,
          address: address.country ? address : null
        })
      });

      const data = await res.json();

      if (!res.ok) {
        setCheckoutResult({
          error: true,
          status: res.status,
          message: data.message || "Unable to process order pricing. Production exception caught by Argus Observer.",
          trace_id: data.trace_id
        });
      } else {
        setCheckoutResult({
          error: false,
          pricing: data.pricing,
          inventory: data.inventory,
          shipping: data.shipping
        });
      }
    } catch (err) {
      setCheckoutResult({
        error: true,
        message: "Failed to connect to checkout service. Please verify the backend is running on port 8001."
      });
    } finally {
      setLoading(false);
    }
  };

  const filteredProducts = PRODUCTS.filter((p) => {
    const matchesCategory = selectedCategory === "All" || p.category === selectedCategory;
    const matchesSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          p.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col font-sans">
      {/* Top Utility Bar */}
      <div className="bg-slate-100 border-b border-slate-200 px-6 py-2 flex items-center justify-between text-xs text-slate-600">
        <div className="flex items-center gap-2">
          <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
          <span className="font-medium">Next.js Monorepo Target for ARGUS Production Debugger</span>
        </div>
        <div className="flex items-center gap-4">
          <span className="hover:text-slate-900 cursor-pointer transition-colors">Customer Support</span>
          <span className="font-mono font-medium">USD ($)</span>
        </div>
      </div>

      {/* Main E-Commerce Header */}
      <header className="border-b border-slate-200 bg-white/90 backdrop-blur-md sticky top-0 z-40 px-6 py-4 flex items-center justify-between gap-4 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center font-bold text-white text-lg shadow-md shadow-indigo-600/20">
            A
          </div>
          <div>
            <h1 className="font-bold text-lg text-slate-900 tracking-tight flex items-center gap-2">
              ArgusStore 
              <span className="text-[11px] font-semibold text-indigo-700 bg-indigo-50 px-2.5 py-0.5 rounded-full border border-indigo-200">
                Next.js App
              </span>
            </h1>
            <p className="text-xs text-slate-500">Developer Gear & Equipment</p>
          </div>
        </div>

        {/* Search Bar */}
        <div className="hidden md:flex flex-1 max-w-md relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search products..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-9 pr-4 py-2 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-500 focus:bg-white transition-all"
          />
        </div>

        <button
          onClick={() => setIsCheckoutOpen(true)}
          className="relative flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2.5 rounded-xl font-medium text-sm transition-all shadow-md shadow-indigo-600/20"
        >
          <ShoppingBag className="w-4 h-4" />
          <span>Cart</span>
          {cart.length > 0 && (
            <span className="bg-emerald-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
              {cart.reduce((a, b) => a + b.quantity, 0)}
            </span>
          )}
        </button>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-8 space-y-8">
        {/* Hero Promotion */}
        <div className="bg-white border border-indigo-100 p-8 rounded-2xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6 shadow-sm">
          <div>
            <span className="text-xs font-bold text-indigo-600 tracking-wider uppercase flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5" /> ARGUS Observer Sandbox Target
            </span>
            <h2 className="text-2xl font-bold text-slate-900 mt-1">E-Commerce Storefront</h2>
            <p className="text-xs text-slate-600 mt-2 max-w-xl leading-relaxed">
              Use promo codes during checkout to test valid discounts or trigger intentional production crashes (<code className="text-indigo-600 bg-indigo-50 px-1 py-0.5 rounded font-mono font-medium">SAVE10</code>, <code className="text-rose-600 bg-rose-50 px-1 py-0.5 rounded font-mono font-medium">INVALID50</code>, or <code className="text-amber-700 bg-amber-50 px-1 py-0.5 rounded font-mono font-medium">FREESHIP100</code>).
            </p>
          </div>
          <div className="flex flex-wrap gap-2 text-xs font-mono">
            <button 
              onClick={() => { setPromoCode("SAVE10"); setIsCheckoutOpen(true); }}
              className="bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 px-3.5 py-2 rounded-xl text-indigo-700 font-semibold transition-colors shadow-sm"
            >
              SAVE10 (10% OFF)
            </button>
            <button 
              onClick={() => { setPromoCode("INVALID50"); setIsCheckoutOpen(true); }}
              className="bg-rose-50 hover:bg-rose-100 border border-rose-200 px-3.5 py-2 rounded-xl text-rose-700 font-semibold transition-colors shadow-sm"
            >
              INVALID50 (KeyError)
            </button>
            <button 
              onClick={() => { setPromoCode("FREESHIP100"); setIsCheckoutOpen(true); }}
              className="bg-amber-50 hover:bg-amber-100 border border-amber-200 px-3.5 py-2 rounded-xl text-amber-800 font-semibold transition-colors shadow-sm"
            >
              FREESHIP100 (ZeroDiv)
            </button>
          </div>
        </div>

        {/* Category Filters */}
        <div className="flex items-center justify-between border-b border-slate-200 pb-4">
          <div className="flex items-center gap-2 overflow-x-auto">
            {["All", "Apparel", "Hardware", "Electronics", "Accessories"].map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-4 py-2 rounded-xl text-xs font-medium transition-all ${
                  selectedCategory === cat
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/30'
                    : 'bg-white hover:bg-slate-100 border border-slate-200 text-slate-600'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
          <span className="text-xs text-slate-500 font-mono hidden sm:inline">{filteredProducts.length} Products</span>
        </div>

        {/* Product Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {filteredProducts.map((prod) => (
            <div key={prod.id} className="bg-white border border-slate-200 rounded-2xl overflow-hidden hover:border-indigo-300 hover:shadow-md transition-all flex flex-col justify-between group">
              <div>
                <div className="h-52 overflow-hidden bg-slate-100 relative">
                  <img src={prod.image} alt={prod.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                  <span className="absolute top-3 right-3 bg-white/90 backdrop-blur-sm text-indigo-700 text-[11px] px-2.5 py-1 rounded-full font-mono font-medium border border-indigo-100 shadow-sm">
                    {prod.category}
                  </span>
                </div>
                <div className="p-5">
                  <div className="flex items-center gap-1 text-amber-500 text-xs mb-1.5">
                    <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                    <span className="font-semibold text-slate-800">{prod.rating}</span>
                    <span className="text-slate-400 text-[11px]">({prod.reviewsCount})</span>
                  </div>
                  <h3 className="font-semibold text-slate-900 text-base leading-snug">{prod.name}</h3>
                  <p className="text-xs text-slate-500 mt-2 line-clamp-2 leading-relaxed">{prod.description}</p>
                </div>
              </div>

              <div className="p-5 pt-0 flex items-center justify-between border-t border-slate-100 mt-4">
                <div>
                  <span className="text-[10px] text-slate-400 block uppercase font-mono font-medium">Price</span>
                  <span className="text-lg font-bold text-slate-900 font-mono">${prod.price.toFixed(2)}</span>
                </div>
                <button
                  onClick={() => addToCart(prod)}
                  className="bg-slate-900 hover:bg-indigo-600 text-white px-4 py-2 rounded-xl text-xs font-semibold transition-all shadow-sm"
                >
                  Add to Cart
                </button>
              </div>
            </div>
          ))}
        </div>
      </main>

      {/* Cart & Checkout Slide-Over Drawer */}
      {isCheckoutOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex justify-end">
          <div className="bg-white border-l border-slate-200 w-full max-w-md h-full flex flex-col justify-between p-6 overflow-y-auto shadow-2xl">
            <div>
              <div className="flex items-center justify-between pb-4 border-b border-slate-200">
                <h3 className="font-bold text-lg text-slate-900 flex items-center gap-2">
                  <ShoppingBag className="w-5 h-5 text-indigo-600" />
                  Your Shopping Cart
                </h3>
                <button 
                  onClick={() => setIsCheckoutOpen(false)} 
                  className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Items List */}
              <div className="divide-y divide-slate-100 my-4 max-h-60 overflow-y-auto pr-1">
                {cart.length === 0 ? (
                  <p className="text-sm text-slate-500 py-8 text-center">Your shopping cart is empty.</p>
                ) : (
                  cart.map((item) => (
                    <div key={item.id} className="py-3 flex items-center justify-between text-sm">
                      <div>
                        <p className="font-medium text-slate-900">{item.name}</p>
                        <p className="text-xs text-slate-500">${item.price.toFixed(2)} each</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="flex items-center bg-slate-100 border border-slate-200 rounded-lg">
                          <button onClick={() => updateQuantity(item.id, -1)} className="px-2.5 py-0.5 text-slate-600 hover:text-slate-900 font-medium">-</button>
                          <span className="px-2 text-xs font-mono font-bold text-slate-900">{item.quantity}</span>
                          <button onClick={() => updateQuantity(item.id, 1)} className="px-2.5 py-0.5 text-slate-600 hover:text-slate-900 font-medium">+</button>
                        </div>
                        <span className="font-mono text-slate-900 font-semibold w-16 text-right">${(item.price * item.quantity).toFixed(2)}</span>
                        <button onClick={() => removeFromCart(item.id)} className="text-rose-500 hover:text-rose-700 text-xs p-1">✕</button>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* Checkout Input Form */}
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-4 my-4">
                <div>
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1 mb-1.5">
                    <Tag className="w-3.5 h-3.5 text-indigo-600" /> Promo Code
                  </label>
                  <input
                    type="text"
                    placeholder="Enter coupon code (e.g. SAVE10, INVALID50)"
                    value={promoCode}
                    onChange={(e) => setPromoCode(e.target.value)}
                    className="w-full bg-white border border-slate-300 rounded-lg px-3 py-2 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-500 font-mono uppercase"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-700 mb-1.5 block">Shipping Address</label>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      placeholder="Country (e.g. US)"
                      value={address.country}
                      onChange={(e) => setAddress({ ...address, country: e.target.value })}
                      className="bg-white border border-slate-300 rounded-lg px-3 py-2 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-500"
                    />
                    <input
                      type="text"
                      placeholder="Postal / Zip Code"
                      value={address.zip_code}
                      onChange={(e) => setAddress({ ...address, zip_code: e.target.value })}
                      className="bg-white border border-slate-300 rounded-lg px-3 py-2 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-500"
                    />
                  </div>
                </div>
              </div>

              {/* Standard Customer Response Alert */}
              {checkoutResult && (
                <div className={`p-4 rounded-xl text-xs ${checkoutResult.error ? 'bg-rose-50 border border-rose-200 text-rose-900' : 'bg-emerald-50 border border-emerald-200 text-emerald-900'}`}>
                  {checkoutResult.error ? (
                    <div className="space-y-1">
                      <strong className="font-bold flex items-center gap-1.5 text-rose-700">
                        <AlertTriangle className="w-4 h-4 text-rose-600" /> Checkout Error (Intercepted by Argus)
                      </strong>
                      <p className="text-rose-800 leading-relaxed text-[11px]">{checkoutResult.message}</p>
                      {checkoutResult.trace_id && (
                        <p className="text-[10px] font-mono text-indigo-700 mt-1">Trace ID: {checkoutResult.trace_id}</p>
                      )}
                    </div>
                  ) : (
                    <div className="space-y-1">
                      <strong className="font-bold flex items-center gap-1.5 text-emerald-700">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Order Placed Successfully!
                      </strong>
                      <p className="text-xs text-slate-700">Total Charged: <span className="font-mono font-bold text-slate-900">${checkoutResult.pricing.total.toFixed(2)}</span></p>
                      {checkoutResult.pricing.discount_amount > 0 && (
                        <p className="text-[11px] text-emerald-700 font-medium">Discount Applied: -${checkoutResult.pricing.discount_amount.toFixed(2)}</p>
                      )}
                      {checkoutResult.shipping && (
                        <p className="text-[10px] text-slate-500 font-mono">Carrier: {checkoutResult.shipping.carrier} (${checkoutResult.shipping.shipping_fee})</p>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Bottom Order Summary & Action */}
            <div className="border-t border-slate-200 pt-4 mt-6">
              <div className="flex items-center justify-between text-sm mb-4">
                <span className="text-slate-600 font-medium">Cart Subtotal:</span>
                <span className="font-mono text-slate-900 font-bold text-base">${cartSubtotal.toFixed(2)}</span>
              </div>

              <button
                onClick={handleCheckout}
                disabled={loading || cart.length === 0}
                className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-200 disabled:text-slate-400 text-white font-semibold py-3 rounded-xl flex items-center justify-center gap-2 transition-all shadow-md shadow-indigo-600/20"
              >
                {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                <span>{loading ? "Processing Order..." : "Place Order & Calculate"}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white px-6 py-6 text-center text-xs text-slate-500 font-mono">
        ArgusStore Target App &copy; 2026. Powered by Next.js App Router + FastAPI Backend.
      </footer>
    </div>
  );
}
