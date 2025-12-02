<template>
  <n-config-provider>
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui'
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: v-sans, system-ui, -apple-system;
}
</style>
