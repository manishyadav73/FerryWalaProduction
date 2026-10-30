export const CATEGORIES = [
  { value: 'VEGETABLES', label: 'Vegetables', short: 'Sabji', color: '#2F6B3A' },
  { value: 'FRUITS', label: 'Fruits', short: 'Fruits', color: '#D8572A' },
  { value: 'DAIRY', label: 'Dairy & eggs', short: 'Dairy', color: '#3F7CAC' },
  { value: 'TEA', label: 'Chai & tea', short: 'Chai', color: '#9A5B2E' },
  { value: 'JUICE', label: 'Juice', short: 'Juice', color: '#E08A12' },
  { value: 'SNACKS', label: 'Snacks', short: 'Snacks', color: '#B8432F' },
  { value: 'GROCERY', label: 'Grocery', short: 'Grocery', color: '#6B5B95' },
  { value: 'OTHER', label: 'Other', short: 'Other', color: '#5C6B5E' },
]

export const categoryLabel = (v) => CATEGORIES.find((c) => c.value === v)?.label ?? 'Other'
export const categoryColor = (v) => CATEGORIES.find((c) => c.value === v)?.color ?? '#5C6B5E'

/** One-tap starters so a vendor can build a stall list in seconds. */
export const QUICK_PRODUCTS = [
  { name: 'Tomato', category: 'VEGETABLES', unit: 'kg' },
  { name: 'Potato', category: 'VEGETABLES', unit: 'kg' },
  { name: 'Onion', category: 'VEGETABLES', unit: 'kg' },
  { name: 'Spinach', category: 'VEGETABLES', unit: 'bunch' },
  { name: 'Banana', category: 'FRUITS', unit: 'dozen' },
  { name: 'Apple', category: 'FRUITS', unit: 'kg' },
  { name: 'Lemon', category: 'VEGETABLES', unit: '6 pcs' },
  { name: 'Coriander', category: 'VEGETABLES', unit: 'bunch' },
]
