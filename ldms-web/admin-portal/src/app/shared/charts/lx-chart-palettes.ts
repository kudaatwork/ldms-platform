/** Chart colors aligned with admin portal design tokens (`src/styles/_tokens.scss`). */
export const LX_CHART_FONT = "'Plus Jakarta Sans', sans-serif";

export const LX_CHART_COLORS = {
  primary: '#3B82F6',
  primaryLight: '#60A5FA',
  analytics: '#8B5CF6',
  analyticsDeep: '#6366F1',
  success: '#22C55E',
  warning: '#F59E0B',
  danger: '#EF4444',
  info: '#0EA5E9',
  teal: '#14B8A6',
  rose: '#F43F5E',
  grid: 'rgba(148, 163, 184, 0.12)',
  tick: '#94A3B8',
  legend: '#64748B',
  tooltipBg: 'rgba(15, 23, 42, 0.92)',
  donutBorder: 'rgba(255, 255, 255, 0.85)',
  series: ['#3B82F6', '#8B5CF6', '#22C55E', '#F59E0B', '#EF4444', '#0EA5E9', '#14B8A6', '#F43F5E'],
  kycPipeline: {
    draft: '#94A3B8',
    submitted: '#F59E0B',
    stage1: '#3B82F6',
    stage2: '#8B5CF6',
    stage3: '#6366F1',
    stage4: '#0EA5E9',
    stage5: '#14B8A6',
    approved: '#22C55E',
    rejected: '#EF4444',
  },
  revenue: {
    earned: 'rgba(59, 130, 246, 0.88)',
    earnedHover: '#2563EB',
    deposits: 'rgba(245, 158, 11, 0.88)',
    depositsHover: '#D97706',
    usage: 'rgba(34, 197, 94, 0.88)',
    usageHover: '#16A34A',
    costs: 'rgba(244, 63, 94, 0.78)',
    costsHover: '#E11D48',
  },
} as const;

export const LX_CHART_ANIMATION = {
  duration: 900,
  easing: 'easeOutQuart' as const,
};
