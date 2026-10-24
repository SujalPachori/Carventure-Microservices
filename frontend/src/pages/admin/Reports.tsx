// import { useQuery } from '@tanstack/react-query'; import { adminApi } from '../../api/admin'; import { Page,PageHeader } from '../../components/common/Page'; import { ErrorState,Loading,EmptyState } from '../../components/common/UI'; import { money,titleCase } from '../../utils/format';
// export function Reports(){const q=useQuery({queryKey:['reports'],queryFn:adminApi.reports});return <Page><PageHeader eyebrow="Admin" title="Reports" description="Metrics returned by the Admin Reporting service."/>{q.isLoading?<Loading/>:q.isError?<ErrorState message="Could not load reports."/>:q.data?.length?<div className="report-detail-grid">{q.data.map(r=><div className="card" key={r.reportType}><h2>{titleCase(r.reportType)}</h2><div className="metric-list">{Object.entries(r.metrics).map(([k,v])=><div key={k}><span>{titleCase(k)}</span><strong>{/value|amount|price/i.test(k)?money(v):v.toLocaleString()}</strong></div>)}</div></div>)}</div>:<EmptyState message="No reports returned."/>}</Page>}

import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/admin';
import { Page, PageHeader } from '../../components/common/Page';
import { ErrorState, Loading, EmptyState } from '../../components/common/UI';
import { money, titleCase } from '../../utils/format';
import { isActiveReportType } from '../../utils/reportTypes';

export function Reports() {
  const q = useQuery({ queryKey: ['reports'], queryFn: adminApi.reports });
  const activeReports = q.data?.filter((r) => isActiveReportType(r.reportType));

  return (
    <Page>
      <PageHeader eyebrow="Admin" title="Reports" description="Metrics returned by the Admin Reporting service." />
      {q.isLoading ? (
        <Loading />
      ) : q.isError ? (
        <ErrorState message="Could not load reports." />
      ) : activeReports?.length ? (
        <div className="report-detail-grid">
          {activeReports.map((r) => (
            <div className="card" key={r.reportType}>
              <h2>{titleCase(r.reportType)}</h2>
              <div className="metric-list">
                {Object.entries(r.metrics).map(([k, v]) => (
                  <div key={k}>
                    <span>{titleCase(k)}</span>
                    <strong>{/value|amount|price/i.test(k) ? money(v) : v.toLocaleString()}</strong>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState message="No reports returned." />
      )}
    </Page>
  );
}