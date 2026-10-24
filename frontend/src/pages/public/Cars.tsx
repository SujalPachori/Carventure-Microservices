import { useQuery } from '@tanstack/react-query';
import { Search, SlidersHorizontal, ChevronDown } from 'lucide-react';
import { useState, useEffect } from 'react';
import { carApi } from '../../api/car';
import { Page, PageHeader } from '../../components/common/Page';
import { CarCard } from '../../components/car/CarCard';
import { EmptyState, ErrorState, Loading } from '../../components/common/UI';
import type { FuelType } from '../../types';

export function Cars(){
  const [filters, setFilters] = useState({
    brand: '', model: '', minPrice: '', maxPrice: '',
    year: '', maxMileage: '', fuelType: '', transmission: '', location: ''
  });
  const [applied, setApplied] = useState(filters);
  const [expanded, setExpanded] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setApplied(filters), 400);
    return () => clearTimeout(timer);
  }, [filters]);

  const q = useQuery({
    queryKey: ['cars', applied],
    queryFn: () => carApi.search(
      Object.fromEntries(
        Object.entries(applied)
          .filter(([, v]) => v !== '')
          .map(([k, v]) => [k, ['minPrice', 'maxPrice', 'year', 'maxMileage'].includes(k) ? Number(v) : v])
      )
    )
  });

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setFilters(f => ({ ...f, [k]: e.target.value }));

  const activeCount = Object.values(filters).filter(v => v !== '').length;

  return <Page>
    <PageHeader eyebrow="Inventory" title="Find your next car" description="Search the live inventory exposed by the Car service."/>
    <form className={`filter-panel ${expanded ? '' : 'filter-panel-collapsed'}`} onSubmit={e => e.preventDefault()}>
      <button
        type="button"
        className="filter-title filter-toggle"
        onClick={() => setExpanded(v => !v)}
        aria-expanded={expanded}
      >
        <span className="filter-title-left">
          <SlidersHorizontal/> Refine your search
          {activeCount > 0 && <span className="filter-count">{activeCount}</span>}
        </span>
        <ChevronDown className={expanded ? 'chevron-open' : ''}/>
      </button>
      {expanded && (
        <div className="filter-grid">
          <input placeholder="Brand" value={filters.brand} onChange={set('brand')}/>
          <input placeholder="Model" value={filters.model} onChange={set('model')}/>
          <input type="number" placeholder="Min price" value={filters.minPrice} onChange={set('minPrice')}/>
          <input type="number" placeholder="Max price" value={filters.maxPrice} onChange={set('maxPrice')}/>
          <input type="number" placeholder="Year" value={filters.year} onChange={set('year')}/>
          <input type="number" placeholder="Max mileage" value={filters.maxMileage} onChange={set('maxMileage')}/>
          <select value={filters.fuelType} onChange={set('fuelType')}>
            <option value="">Fuel type</option>
            {(['PETROL','DIESEL','CNG'] as FuelType[]).map(x => <option key={x}>{x}</option>)}
          </select>
          <input placeholder="Transmission" value={filters.transmission} onChange={set('transmission')}/>
          <input placeholder="Location" value={filters.location} onChange={set('location')}/>
        </div>
      )}
    </form>
    {q.isLoading ? <Loading label="Loading inventory…"/> :
     q.isError ? <ErrorState message="Could not load cars."/> :
     q.data?.length ? <div className="car-grid">{q.data.map(c => <CarCard key={c.carId} car={c}/>)}</div> :
     <EmptyState message="No cars match those filters."/>}
  </Page>
}