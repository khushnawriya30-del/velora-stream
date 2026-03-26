/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#0A0A0A',
        surface: '#141414',
        'surface-light': '#1E1E1E',
        border: '#2A2A2A',
        gold: '#F5A623',
        'gold-light': '#FFC857',
        'gold-dark': '#D4891A',
        'text-primary': '#FFFFFF',
        'text-secondary': '#A0A0A0',
        'text-muted': '#6B6B6B',
        error: '#E74C3C',
        success: '#2ECC71',
        warning: '#F39C12',
        info: '#3498DB',
      },
      fontFamily: {
        sans: ['DM Sans', 'system-ui', 'sans-serif'],
        display: ['Playfair Display', 'serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
  plugins: [],
};
