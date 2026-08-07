import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";
import type { TokenPair } from "../types";

// expo-secure-store는 web을 지원하지 않는다 (SDK 57 문서) — native는 SecureStore, web은 localStorage로 분기.
const ACCESS_KEY = "alrdream_access_token";
const REFRESH_KEY = "alrdream_refresh_token";

async function getItem(key: string): Promise<string | null> {
  if (Platform.OS === "web") {
    return typeof localStorage !== "undefined" ? localStorage.getItem(key) : null;
  }
  return SecureStore.getItemAsync(key);
}

async function setItem(key: string, value: string): Promise<void> {
  if (Platform.OS === "web") {
    if (typeof localStorage !== "undefined") localStorage.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

async function deleteItem(key: string): Promise<void> {
  if (Platform.OS === "web") {
    if (typeof localStorage !== "undefined") localStorage.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
}

export const tokenStorage = {
  async load(): Promise<TokenPair | null> {
    const [accessToken, refreshToken] = await Promise.all([getItem(ACCESS_KEY), getItem(REFRESH_KEY)]);
    if (!accessToken || !refreshToken) return null;
    return { accessToken, refreshToken };
  },
  async save(tokens: TokenPair): Promise<void> {
    await Promise.all([setItem(ACCESS_KEY, tokens.accessToken), setItem(REFRESH_KEY, tokens.refreshToken)]);
  },
  async clear(): Promise<void> {
    await Promise.all([deleteItem(ACCESS_KEY), deleteItem(REFRESH_KEY)]);
  },
};
