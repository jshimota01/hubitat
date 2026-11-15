export async function sendHubitatCommand(device, command) {
  if (!device || device.backend !== 'hubitat' || !device.commandUrl) throw new Error('Hubitat command target is not configured');
  const response = await fetch(device.commandUrl, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ endpointId: device.endpointId, hubitatDeviceId: device.hubitatDeviceId, command }) ,
    signal: AbortSignal.timeout(8000)
  });
  if (!response.ok) throw new Error(`Hubitat command endpoint returned HTTP ${response.status}`);
  return true;
}
