import pluginVue from 'eslint-plugin-vue'

export default [
  {
    ignores: ['dist/**', 'dist-ssr/**', 'coverage/**', 'node_modules/**']
  },
  {
    files: ['**/*.{js,mjs,cjs,vue}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module'
    },
    plugins: {
      vue: pluginVue
    },
    rules: {
      ...pluginVue.configs['flat/recommended'].rules,
      'vue/multi-word-component-names': 'off'
    }
  }
]
