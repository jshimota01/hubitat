import { Router } from "express";
import { config } from "../config.js";
const router = Router();

function requireApiKey(req, res, next) {
  const supplied = req.get("authorization") || "";
  const expected = `Bearer ${config.apiKey}`;
  if (!config.apiKey || supplied.length !== expected.length ||
      !Buffer.from(supplied).equals(Buffer.from(expected))) {
    return res.status(401).json({ error: "unauthorized" });
  }
  next();
}

router.get("/health", (_req, res) => res.json({
  service: "alexa-manager-advanced", version: "0.2.1",
  status: "ok", timestamp: new Date().toISOString()
}));

router.get("/v1/status", requireApiKey, (_req, res) => res.json({
  service: "alexa-manager-advanced", version: "0.2.1",
  phase: "oauth-foundation", amazonSkill: "not-configured",
  oauth: "local-foundation", hubitatBackend: "not-configured"
}));

// Reserved Lambda-to-NAS contract. Full directive handling comes in a later phase.
router.post("/v1/alexa/directive", requireApiKey, (_req, res) => res.status(501).json({
  error: "not_implemented",
  message: "Alexa directive processing is not enabled in this foundation build."
}));
export default router;
