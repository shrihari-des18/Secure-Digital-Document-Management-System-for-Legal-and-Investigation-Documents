import { apiClient } from '@/lib/api-client';
import type { SecurityAlarm, TriggerTestAlarmRequest } from '@/types';

export interface ListAlarmsParams {
  unacknowledgedOnly?: boolean;
  severity?: string;
  caseId?: string;
}

export const alarmsApi = {
  /**
   * GET /api/v1/alarms
   * List security alarms with optional filters.
   */
  listAlarms(params: ListAlarmsParams = {}): Promise<SecurityAlarm[]> {
    const searchParams = new URLSearchParams();
    if (params.unacknowledgedOnly) searchParams.set('unacknowledgedOnly', 'true');
    if (params.severity) searchParams.set('severity', params.severity);
    if (params.caseId) searchParams.set('caseId', params.caseId);
    const qs = searchParams.toString();
    return apiClient.get<SecurityAlarm[]>(`/alarms${qs ? `?${qs}` : ''}`);
  },

  /**
   * GET /api/v1/alarms/unacknowledged-count
   * Returns the total count of unacknowledged alarms for badge indicators.
   */
  getUnacknowledgedCount(): Promise<number> {
    return apiClient.get<number>('/alarms/unacknowledged-count');
  },

  /**
   * GET /api/v1/alarms/{id}
   * Retrieve full details of a specific alarm.
   */
  getAlarmById(id: string): Promise<SecurityAlarm> {
    return apiClient.get<SecurityAlarm>(`/alarms/${id}`);
  },

  /**
   * POST /api/v1/alarms/{id}/acknowledge
   * Acknowledge a security alarm.
   */
  acknowledgeAlarm(id: string): Promise<SecurityAlarm> {
    return apiClient.post<SecurityAlarm>(`/alarms/${id}/acknowledge`);
  },

  /**
   * POST /api/v1/alarms/test-trigger
   * Trigger a simulated security drill alarm (SYSTEM_ADMIN only).
   */
  triggerTestAlarm(payload: TriggerTestAlarmRequest): Promise<SecurityAlarm> {
    return apiClient.post<SecurityAlarm>('/alarms/test-trigger', payload);
  },
};
