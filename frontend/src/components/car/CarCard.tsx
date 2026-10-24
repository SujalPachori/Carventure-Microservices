import { Link } from 'react-router-dom'
import { MapPin, Gauge, Fuel, Settings2 } from 'lucide-react'
import type { Car } from '../../types'
import { money } from '../../utils/format'
import { CarImage } from './CarImage'
export function CarCard({car}:{car:Car}){const image=car.imageUrls?.[0];return <Link className="car-card" to={`/cars/${car.carId}`}><div className="car-image">{image?<CarImage src={image} alt={`${car.brand} ${car.model}`}/>:<div className="car-placeholder">CARVENTURE</div>}<span className="image-status">{car.status||'Listed'}</span></div><div className="car-body"><div className="eyebrow">{car.manufactureYear} · {car.condition||'Used'}</div><h3>{car.brand} {car.model}</h3><div className="price">{money(car.price)}</div><div className="specs">{car.mileage!=null&&<span><Gauge/> {car.mileage.toLocaleString()} km</span>}{car.fuelType&&<span><Fuel/> {car.fuelType}</span>}{car.transmission&&<span><Settings2/> {car.transmission}</span>}</div>{car.location&&<div className="muted"><MapPin/> {car.location}</div>}</div></Link>}
