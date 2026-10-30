import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'

const KEY = 'fw_cart'
const CartContext = createContext(null)

function load() {
  try {
    const c = JSON.parse(localStorage.getItem(KEY))
    if (c && c.vendorId && Array.isArray(c.items)) return c
  } catch { /* ignore */ }
  return { vendorId: null, vendorName: '', items: [] }
}

/** A request goes to ONE vendor, so the cart holds one vendor's items. */
export function CartProvider({ children }) {
  const [cart, setCart] = useState(load)

  useEffect(() => {
    try { localStorage.setItem(KEY, JSON.stringify(cart)) } catch { /* private mode */ }
  }, [cart])

  const add = useCallback((vendor, product) => {
    if (cart.vendorId && cart.vendorId !== vendor.id && cart.items.length > 0) return 'conflict'
    setCart((c) => {
      const items = [...c.items]
      const i = items.findIndex((x) => x.productId === product.id)
      const max = Math.min(50, product.stock)
      if (i >= 0) items[i] = { ...items[i], quantity: Math.min(max, items[i].quantity + 1), max }
      else items.push({ productId: product.id, name: product.name, unit: product.unit, price: product.price, quantity: 1, max })
      return { vendorId: vendor.id, vendorName: vendor.shopName, items }
    })
    return 'ok'
  }, [cart.vendorId, cart.items.length])

  const startNew = useCallback((vendor, product) => {
    setCart({
      vendorId: vendor.id,
      vendorName: vendor.shopName,
      items: [{ productId: product.id, name: product.name, unit: product.unit, price: product.price, quantity: 1, max: Math.min(50, product.stock) }],
    })
  }, [])

  const setQty = useCallback((productId, quantity) => {
    setCart((c) => {
      const items = c.items
        .map((x) => (x.productId === productId ? { ...x, quantity: Math.min(x.max, quantity) } : x))
        .filter((x) => x.quantity > 0)
      return items.length ? { ...c, items } : { vendorId: null, vendorName: '', items: [] }
    })
  }, [])

  const clear = useCallback(() => setCart({ vendorId: null, vendorName: '', items: [] }), [])

  const value = useMemo(() => ({
    ...cart,
    count: cart.items.reduce((n, x) => n + x.quantity, 0),
    total: cart.items.reduce((n, x) => n + Number(x.price) * x.quantity, 0),
    qtyOf: (productId) => cart.items.find((x) => x.productId === productId)?.quantity ?? 0,
    add, startNew, setQty, clear,
  }), [cart, add, startNew, setQty, clear])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCart() {
  return useContext(CartContext)
}
