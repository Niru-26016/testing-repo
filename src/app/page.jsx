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
  SlidersHorizontal,
  Star
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

export default function Storefront() {
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
        // Standard e-commerce error handling for end users
        setCheckoutResult({
          error: true,
          status: res.status,
          message: "Unable to process order pricing. Please verify your promo code or address."
        });
      } else {
        setCheckoutResult({
          error: false,
          pricing: data.pricing
        });
      }
    } catch (err) {
      setCheckoutResult({
        error: true,
        message: "Failed to connect to checkout service. Please try again."
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
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Utility Bar */}
      <div className="bg-slate-900 border-b border-slate-800 px-6 py-2 flex items-center justify-between text-xs text-slate-400">
        <div>Free shipping on orders over $50</div>
        <div className="flex items-center gap-4">
          <span>Customer Support</span>
          <span>USD ($)</span>
        </div>
      </div>

      {/* Main E-Commerce Header */}
      <header className="border-b border-slate-800 bg-slate-900/90 backdrop-blur-md sticky top-0 z-40 px-6 py-4 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center font-bold text-white text-lg shadow-md shadow-indigo-600/30">
            D
          </div>
          <div>
            <h1 className="font-bold text-lg text-white tracking-tight">DemoStore</h1>
            <p className="text-xs text-slate-400">Developer Gear & Equipment</p>
          </div>
        </div>

        {/* Search Bar */}
        <div className="hidden md:flex flex-1 max-w-md relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-2.5" />
          <input
            type="text"
            placeholder="Search products..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <button
          onClick={() => setIsCheckoutOpen(true)}
          className="relative flex items-center gap-2.5 bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2.5 rounded-xl font-medium text-sm transition-all shadow-md shadow-indigo-600/20"
        >
          <ShoppingBag className="w-4 h-4" />
          <span>Cart</span>
          {cart.length > 0 && (
            <span className="bg-emerald-400 text-slate-950 text-xs font-bold px-2 py-0.5 rounded-full">
              {cart.reduce((a, b) => a + b.quantity, 0)}
            </span>
          )}
        </button>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-8 space-y-8">
        {/* Hero Promotion */}
        <div className="bg-gradient-to-r from-indigo-950 via-slate-900 to-slate-950 border border-indigo-500/20 p-8 rounded-2xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div>
            <span className="text-xs font-semibold text-indigo-400 tracking-wider uppercase">Limited Time Offers</span>
            <h2 className="text-2xl font-bold text-white mt-1">Upgrade Your Workspace Setup</h2>
            <p className="text-xs text-slate-400 mt-2 max-w-xl">
              Apply coupon codes during checkout to receive store discounts. Use promo codes like <code className="text-indigo-300 font-mono">SAVE10</code> or <code className="text-indigo-300 font-mono">SAVE20</code>.
            </p>
          </div>
          <div className="flex gap-2 text-xs font-mono">
            <span className="bg-slate-900 border border-slate-700 px-3 py-2 rounded-xl text-indigo-300">
              SAVE10 (10% OFF)
            </span>
            <span className="bg-slate-900 border border-slate-700 px-3 py-2 rounded-xl text-indigo-300">
              SAVE20 (20% OFF)
            </span>
          </div>
        </div>

        {/* Category Filters */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-2 overflow-x-auto">
            {["All", "Apparel", "Hardware", "Electronics", "Accessories"].map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-4 py-2 rounded-xl text-xs font-medium transition-all ${
                  selectedCategory === cat
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                    : 'bg-slate-900 hover:bg-slate-800 text-slate-400'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
          <span className="text-xs text-slate-400 font-mono hidden sm:inline">{filteredProducts.length} Products</span>
        </div>

        {/* Product Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {filteredProducts.map((prod) => (
            <div key={prod.id} className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden hover:border-slate-700 transition-all flex flex-col justify-between group">
              <div>
                <div className="h-52 overflow-hidden bg-slate-800 relative">
                  <img src={prod.image} alt={prod.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                  <span className="absolute top-3 right-3 bg-slate-950/80 backdrop-blur-sm text-indigo-300 text-[11px] px-2.5 py-1 rounded-full font-mono border border-indigo-500/20">
                    {prod.category}
                  </span>
                </div>
                <div className="p-5">
                  <div className="flex items-center gap-1 text-amber-400 text-xs mb-1">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <span className="font-semibold text-slate-200">{prod.rating}</span>
                    <span className="text-slate-500 text-[11px]">({prod.reviewsCount})</span>
                  </div>
                  <h3 className="font-semibold text-white text-base leading-snug">{prod.name}</h3>
                  <p className="text-xs text-slate-400 mt-2 line-clamp-2">{prod.description}</p>
                </div>
              </div>

              <div className="p-5 pt-0 flex items-center justify-between border-t border-slate-800/60 mt-4">
                <div>
                  <span className="text-[11px] text-slate-500 block uppercase font-mono">Price</span>
                  <span className="text-lg font-bold text-white font-mono">${prod.price.toFixed(2)}</span>
                </div>
                <button
                  onClick={() => addToCart(prod)}
                  className="bg-slate-800 hover:bg-indigo-600 text-slate-200 hover:text-white px-3.5 py-2 rounded-xl text-xs font-semibold transition-all"
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
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex justify-end">
          <div className="bg-slate-900 border-l border-slate-800 w-full max-w-md h-full flex flex-col justify-between p-6 overflow-y-auto">
            <div>
              <div className="flex items-center justify-between pb-4 border-b border-slate-800">
                <h3 className="font-bold text-lg text-white flex items-center gap-2">
                  <ShoppingBag className="w-5 h-5 text-indigo-400" />
                  Your Shopping Cart
                </h3>
                <button onClick={() => setIsCheckoutOpen(false)} className="text-slate-400 hover:text-white text-sm">✕ Close</button>
              </div>

              {/* Items List */}
              <div className="divide-y divide-slate-800 my-4 max-h-60 overflow-y-auto pr-1">
                {cart.length === 0 ? (
                  <p className="text-sm text-slate-400 py-8 text-center">Your shopping cart is empty.</p>
                ) : (
                  cart.map((item) => (
                    <div key={item.id} className="py-3 flex items-center justify-between text-sm">
                      <div>
                        <p className="font-medium text-white">{item.name}</p>
                        <p className="text-xs text-slate-400">${item.price.toFixed(2)} each</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="flex items-center bg-slate-950 border border-slate-800 rounded-lg">
                          <button onClick={() => updateQuantity(item.id, -1)} className="px-2.5 py-0.5 text-slate-400 hover:text-white">-</button>
                          <span className="px-2 text-xs font-mono font-bold text-white">{item.quantity}</span>
                          <button onClick={() => updateQuantity(item.id, 1)} className="px-2.5 py-0.5 text-slate-400 hover:text-white">+</button>
                        </div>
                        <span className="font-mono text-slate-200 font-semibold w-16 text-right">${(item.price * item.quantity).toFixed(2)}</span>
                        <button onClick={() => removeFromCart(item.id)} className="text-rose-400 hover:text-rose-300 text-xs">✕</button>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* Checkout Input Form */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-4 my-4">
                <div>
                  <label className="text-xs font-medium text-slate-300 flex items-center gap-1 mb-1.5">
                    <Tag className="w-3.5 h-3.5 text-indigo-400" /> Promo Code
                  </label>
                  <input
                    type="text"
                    placeholder="Enter coupon code"
                    value={promoCode}
                    onChange={(e) => setPromoCode(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 font-mono uppercase"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-300 mb-1.5 block">Shipping Address</label>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      placeholder="Country (e.g. US)"
                      value={address.country}
                      onChange={(e) => setAddress({ ...address, country: e.target.value })}
                      className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
                    />
                    <input
                      type="text"
                      placeholder="Postal / Zip Code"
                      value={address.zip_code}
                      onChange={(e) => setAddress({ ...address, zip_code: e.target.value })}
                      className="bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
                    />
                  </div>
                </div>
              </div>

              {/* Standard Customer Response Alert */}
              {checkoutResult && (
                <div className={`p-4 rounded-xl text-xs ${checkoutResult.error ? 'bg-rose-500/10 border border-rose-500/30 text-rose-200' : 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300'}`}>
                  {checkoutResult.error ? (
                    <div className="space-y-1">
                      <strong className="font-bold flex items-center gap-1.5 text-rose-400">
                        <AlertTriangle className="w-4 h-4 text-rose-400" /> Checkout Error
                      </strong>
                      <p className="text-slate-300 leading-relaxed text-[11px]">{checkoutResult.message}</p>
                    </div>
                  ) : (
                    <div className="space-y-1">
                      <strong className="font-bold flex items-center gap-1.5 text-emerald-400">
                        <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Order Placed Successfully!
                      </strong>
                      <p className="text-xs text-slate-300">Total Charged: <span className="font-mono font-bold text-white">${checkoutResult.pricing.total.toFixed(2)}</span></p>
                      {checkoutResult.pricing.discount_amount > 0 && (
                        <p className="text-[11px] text-emerald-400">Discount Applied: -${checkoutResult.pricing.discount_amount.toFixed(2)}</p>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Bottom Order Summary & Action */}
            <div className="border-t border-slate-800 pt-4 mt-6">
              <div className="flex items-center justify-between text-sm mb-4">
                <span className="text-slate-400">Cart Subtotal:</span>
                <span className="font-mono text-white font-bold">${cartSubtotal.toFixed(2)}</span>
              </div>

              <button
                onClick={handleCheckout}
                disabled={loading || cart.length === 0}
                className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:bg-slate-800 disabled:text-slate-500 text-white font-semibold py-3 rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg shadow-indigo-600/20"
              >
                {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <ArrowRight className="w-4 h-4" />}
                <span>{loading ? "Processing Order..." : "Place Order & Calculate"}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Footer */}
      <footer className="border-t border-slate-800 px-6 py-6 text-center text-xs text-slate-500 font-mono">
        DemoStore E-Commerce &copy; 2026. All rights reserved.
      </footer>
    </div>
  );
}
