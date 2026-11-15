const port = Number.parseInt(process.env.PORT || "4100", 10);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error("Invalid PORT");
export const config = Object.freeze({
  port, apiKey: process.env.AMA_API_KEY || "",
  publicBaseUrl: process.env.PUBLIC_BASE_URL || "https://alexa.e-wiz.com"
});
