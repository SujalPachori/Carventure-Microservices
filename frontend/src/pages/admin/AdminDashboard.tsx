import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BarChart3, ShieldCheck, Users, CarFront } from 'lucide-react';
import { adminApi } from '../../api/admin';
import { userApi } from '../../api/user';
import { Page, PageHeader } from '../../components/common/Page';
import { ErrorState, Loading } from '../../components/common/UI';
import { titleCase } from '../../utils/format';
import { isActiveReportType } from '../../utils/reportTypes';

const NAV = [
  { to: '/admin/users', icon: Users, label: 'User management', accent: 'amber' },
  { to: '/admin/reports', icon: BarChart3, label: 'Reports', accent: 'teal' },
  { to: '/admin/verify', icon: ShieldCheck, label: 'Listing verification', accent: 'violet' },
  { to: '/admin/test-drives', icon: CarFront, label: 'Test drives', accent: 'red' },
];

// Half-circle instrument gauge — inline styles only, no dependency on
// external CSS vars, so it can't be silently overridden or collide with
// an existing ".gauge" class elsewhere in the app.
function Gauge({ pct, color = '#ffb020', trackColor = '#2a3341' }) {
  const r = 26;
  const circumference = Math.PI * r;
  const offset = circumference * (1 - Math.min(Math.max(pct, 0), 1));
  return (
    <svg
      width="64"
      height="38"
      viewBox="0 0 64 38"
      style={{ display: 'block', overflow: 'visible' }}
      aria-hidden="true"
    >
      <path
        d="M6 34 A26 26 0 0 1 58 34"
        fill="none"
        stroke={trackColor}
        strokeWidth="5"
        strokeLinecap="round"
      />
      <path
        d="M6 34 A26 26 0 0 1 58 34"
        fill="none"
        stroke={color}
        strokeWidth="5"
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={offset}
      />
    </svg>
  );
}

function NavCard({ to, icon: Icon, label, accent, stat, sub, gauge }) {
  return (
    <Link className={`adm-card adm-card--${accent}`} to={to}>
      <div className="adm-card__top">
        <span className="adm-card__icon"><Icon size={18} /></span>
        {gauge != null && <Gauge pct={gauge} color="#ffb020" trackColor="#2a3341" />}
      </div>
      <strong className="adm-card__label">{label}</strong>
      {stat && <span className="adm-card__stat">{stat}</span>}
      {sub && <span className="adm-card__sub">{sub}</span>}
    </Link>
  );
}

function MetricBars({ metrics }) {
  const entries = Object.entries(metrics ?? {});
  const numeric = entries.filter(([, v]) => typeof v === 'number');
  const other = entries.filter(([, v]) => typeof v !== 'number');
  const max = Math.max(1, ...numeric.map(([, v]) => v));

  return (
    <div className="adm-metric-list">
      {numeric.map(([key, value]) => (
        <div className="adm-metric-row" key={key}>
          <span className="adm-metric-row__label">{titleCase(key)}</span>
          <div className="adm-metric-row__track">
            <div className="adm-metric-row__fill" style={{ width: `${(value / max) * 100}%` }} />
          </div>
          <span className="adm-metric-row__value">{value.toLocaleString()}</span>
        </div>
      ))}
      {other.map(([key, value]) => (
        <div className="adm-metric-row adm-metric-row--plain" key={key}>
          <span className="adm-metric-row__label">{titleCase(key)}</span>
          <span className="adm-metric-row__value">{String(value)}</span>
        </div>
      ))}
    </div>
  );
}

export function AdminDashboard() {
  const reports = useQuery({ queryKey: ['reports'], queryFn: adminApi.reports });
  const sellers = useQuery({ queryKey: ['sellers'], queryFn: userApi.sellers });
  const customers = useQuery({ queryKey: ['customers'], queryFn: userApi.customers });

  const customerCount = customers.data?.length ?? 0;
  const sellerCount = sellers.data?.length ?? 0;
  const total = customerCount + sellerCount;
  const customerShare = total > 0 ? customerCount / total : 0;

  const activeReports = reports.data?.filter((r) => isActiveReportType(r.reportType)); // <-- this line


  return (
    <Page>
      <PageHeader
        eyebrow="Admin"
        title="Governance & reporting"
        description="Admin-facing features that are actually exposed by the backend."
      />

      <div className="adm-grid">
        <NavCard
          {...NAV[0]}
          stat={`${customerCount.toLocaleString()} customers`}
          sub={`${sellerCount.toLocaleString()} sellers`}
          gauge={customerShare}
        />
        <NavCard {...NAV[1]} stat={`${activeReports?.length ?? 0} report types`} sub="Live reporting service" />
        <NavCard {...NAV[2]} stat="Verify by ID" sub="Manual review queue" />
        <NavCard {...NAV[3]} stat="All bookings" sub="Test-drive scheduling" />
      </div>

      <section className="adm-section">
        <h2 className="adm-section__title">Reporting service</h2>

        {reports.isLoading ? (
          <Loading />
        ) : reports.isError ? (
          <ErrorState message="Reports are unavailable." />
        ) : (
          <div className="adm-report-grid">
            {activeReports?.map((r) => (
              <div className="adm-report-card" key={r.reportType}>
                <div className="adm-report-card__header">
                  <span>{humanize(r.reportType)}</span>
                  <small>{Object.keys(r.metrics).length ? `${Object.keys(r.metrics).length} metrics` : ''}</small>
                </div>
                <ReportMetrics metrics={r.metrics} />
              </div>
            ))}
          </div>
        )}
      </section>
    </Page>
  );
}

// Splits "ordersByStatus.Awaiting Payment" -> group "ordersByStatus", label "Awaiting Payment".
// Keys with no dot (e.g. "totalOrders") are standalone headline stats.
function parseMetrics(metrics) {
  const totals = [];
  const groups = {};

  for (const [key, value] of Object.entries(metrics ?? {})) {
    const dotIndex = key.indexOf('.');
    if (dotIndex === -1) {
      totals.push({ key, value });
    } else {
      const group = key.slice(0, dotIndex);
      const label = key.slice(dotIndex + 1);
      (groups[group] ??= []).push({ label, value });
    }
  }
  return { totals, groups: Object.entries(groups) };
}

// "ordersByStatus" -> "Orders By Status". Leaves already-human strings
// like "Awaiting Payment" untouched.
function humanize(str) {
  return str
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim()
    .replace(/^\w/, (c) => c.toUpperCase());
}

const GROUP_COLORS = ['#ffb020', '#3ecf8e', '#9b8cff', '#ff6b6b', '#5ac8fa'];

function ReportMetrics({ metrics }) {
  const { totals, groups } = parseMetrics(metrics);

  if (totals.length === 0 && groups.length === 0) {
    return <p className="adm-empty">No metrics recorded yet.</p>;
  }

  return (
    <div className="adm-metrics">
      {totals.length > 0 && (
        <div className="adm-totals">
          {totals.map(({ key, value }) => (
            <div className="adm-total" key={key}>
              <strong>{typeof value === 'number' ? value.toLocaleString() : String(value)}</strong>
              <span>{humanize(key)}</span>
            </div>
          ))}
        </div>
      )}

      {groups.map(([group, items], i) => {
        const color = GROUP_COLORS[i % GROUP_COLORS.length];
        const sum = items.reduce((s, { value }) => s + (typeof value === 'number' ? value : 0), 0);
        return (
          <div className="adm-group" key={group}>
            <h4>{humanize(group)}</h4>
            {items.map(({ label, value }) => {
              const pct = sum > 0 && typeof value === 'number' ? (value / sum) * 100 : 0;
              return (
                <div className="adm-bar-row" key={label}>
                  <div className="adm-bar-row__meta">
                    <span>{humanize(label)}</span>
                    <span className="adm-bar-row__value">
                      {typeof value === 'number' ? value.toLocaleString() : String(value)}
                      {sum > 0 && <em>{Math.round(pct)}%</em>}
                    </span>
                  </div>
                  <div className="adm-bar-row__track">
                    <div className="adm-bar-row__fill" style={{ width: `${pct}%`, background: color }} />
                  </div>
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}