import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  theme: {
    extend: {
      colors: {
        ink: "#0b1220",
        signal: "#f97316",
        cream: "#f8f5ef"
      },
      boxShadow: {
        glow: "0 24px 80px -36px rgba(249, 115, 22, 0.65)"
      }
    }
  },
  plugins: []
};

export default config;
