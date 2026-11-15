import express from "express";
import helmet from "helmet";
import { config } from "./config.js";
import api from "./routes/api.js";
import { log } from "./logger.js";

const app = express();
app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "256kb", strict: true }));
app.use("/api", api);
app.get("/", (_req, res) => res.type("text/plain").send("Alexa Manager Advanced NAS service"));
app.use((err, _req, res, _next) => {
  log("error", "Request processing error", { message: err.message });
  res.status(400).json({ error: "bad_request" });
});
app.listen(config.port, "0.0.0.0", () => {
  log("info", `Service listening on port ${config.port}`, { publicBaseUrl: config.publicBaseUrl });
});
