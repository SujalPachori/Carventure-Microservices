import { clients } from './client'
import type { Car, CarPayment, CarSchedule } from '../types'
export interface CarFilters {brand?:string;model?:string;minPrice?:number;maxPrice?:number;year?:number;maxMileage?:number;fuelType?:string;transmission?:string;location?:string}
export const carApi = {
  all: () => clients.car.get<Car[]>('/api/v1/car/searchCars').then(r=>r.data),
  get: (id:number) => clients.car.get<Car>(`/api/v1/car/viewCar/${id}`).then(r=>r.data),
  sellerCars: async (username:string) => { const cars = await clients.car.get<Car[]>('/api/v1/car/searchCars').then(r=>r.data); return cars.filter(c=>c.sellerUsername===username) },
  search: (filters:CarFilters) => clients.car.get<Car[]>('/api/v1/car/searchCars',{params:filters}).then(r=>r.data),
  recommendations: (username:string) => clients.car.get<Car[]>(`/api/v1/car/recommendations/${encodeURIComponent(username)}`).then(r=>r.data),
  create: (payload:Partial<Car>) => clients.car.post<Car>('/api/v1/car/addCar',payload).then(r=>r.data),
  update: (id:number,payload:Partial<Car>) => clients.car.put<Car>(`/api/v1/car/updateCar/${id}`,payload).then(r=>r.data),
  status: (id:number,status:string) => clients.car.put<Car>(`/api/v1/car/updateCarStatus/${id}`,null,{params:{status}}).then(r=>r.data),
  remove: (id:number) => clients.car.delete(`/api/v1/car/deleteCar/${id}`),
  suggestPrice: (id:number) => clients.car.get<number>('/api/v1/car/suggestPrice',{params:{carId:id}}).then(r=>r.data),
  uploadDocuments: (id:number,data:FormData) => clients.car.post(`/api/v1/car/uploadCarDocuments/${id}`,data,{headers:{'Content-Type':'multipart/form-data'}}),
  uploadPicture: (id:number,file:File) => { const fd=new FormData(); fd.append('img',file); return clients.car.post(`/api/v1/car/uploadCarPictures/${id}`,fd,{headers:{'Content-Type':'multipart/form-data'}}) },
  deletePicture: (id:number,imgId:number) => clients.car.delete(`/api/v1/car/deleteCarPictures/${id}`,{params:{imgId}}),
  imageBlob: (filename:string) => clients.car.get<Blob>(`/api/v1/car/download/${encodeURIComponent(filename)}`,{responseType:'blob'}).then(r=>r.data),
  paymentsView: (id:number) => clients.car.get<CarPayment>(`/api/v1/car/viewCarPayments/${id}`).then(r=>r.data),
  scheduleView: (id:number) => clients.car.get<CarSchedule>(`/api/v1/car/viewCarSched/${id}`).then(r=>r.data)
}
