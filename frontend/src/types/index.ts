export type Role = 'CUSTOMER' | 'SELLER' | 'ADMIN'
export type FuelType = 'PETROL' | 'DIESEL' | 'CNG'
export type AccountStatus = 'PENDING' | 'APPROVED' | 'SUSPENDED'
export type PaymentType = 'FULL_PAYMENT' | 'FINANCING'
export type OrderStatus = 'CREATED'|'AWAITING_PAYMENT'|'PAID'|'FINANCING_ACTIVE'|'PAYMENT_FAILED'|'CANCELLED'|'EXPIRED'|'REFUNDED'
export type TestDriveStatus = 'PENDING'|'CONFIRMED'|'REJECTED'|'CANCELLED'|'COMPLETED'
export type OfferStatus = 'PENDING'|'ACCEPTED'|'REJECTED'|'COUNTERED'|'CLOSED'
export type OfferedBy = 'CUSTOMER'|'SELLER'
export type DisputeStatus = string
export type VerificationDecision = string

export interface AuthResponse { accessToken:string; refreshToken:string; tokenType:string; username:string; roles:Role[]|Set<Role>; expiresInSeconds:number }
export interface UserProfile { userId?:number; accountStatus?:AccountStatus; username:string; firstName:string; lastName:string; email:string; phoneNumber?:string; address?:string; pincode:string; dateOfBirth?:string }
export interface Car { carId:number; sellerUsername:string; sellerId?:number; brand:string; model:string; manufactureYear:number|string; colour?:string; mileage?:number; fuelType?:FuelType; transmission?:string; price?:number; suggestedPrice?:number; condition?:string; location?:string; kmdriven?:string; pincode?:string; contact?:string; description?:string; postedDate?:string; status?:string; imageUrls?:string[]; rcUrl?:string; insuranceUrl?:string; pucUrl?:string }
export interface CarPayment { id:number; sellerId:number; price:number; listed:boolean; currency:string }
export interface CarSchedule { id:number; sellerUsername:string; name:string; active:boolean }
export interface Availability { id:number; carId:number; sellerUsername:string; dayOfWeek:string; startTime:string; endTime:string; durationMinutes?:number; active:boolean }
export interface TestDrive { id:number; carId:number; sellerUsername:string; buyerUsername:string; startTime:string; endTime:string; status:TestDriveStatus; rejectionReason?:string; cancellationReason?:string; createdAt?:string; updatedAt?:string }
export interface Order { id:number; buyerId:number; sellerId:number; carId:number; totalAmount:number; downPaymentAmount:number; currency:string; paymentType:PaymentType; status:OrderStatus; createdAt:string; updatedAt:string }
export interface OrderWithPayment { order:Order; stripeClientSecret:string; stripePublishableKeyHint:string }
export interface FinancingPlan { id:number; orderId:number; principalAmount:number; tenureMonths:number; monthlyInstallment:number; status:string }
export interface ListingVerification { id?:number; carId:number; decision:string; reason?:string; verifiedByAdminUsername?:string; verifiedDate?:string }
export interface Report { reportType:string; metrics:Record<string,number> }
export interface UserRoles { username:string; roles:Role[]|Set<Role> }
export interface ApiError { status?:number; message:string; details?:string }
export interface Offer { offerId:string; carId:string; sellerId:string; customerId:string; offeredAmount:number; offeredBy:OfferedBy; status:OfferStatus; createdAt:string; carBrand?:string; carModel?:string; carPrice?:number; carImageUrl?:string; customerName?:string }
export interface WishlistItem { wishlistItemId:string; carId:string; addedAt:string; brand?:string; model?:string; manufactureYear?:number; price?:number; imageUrl?:string; carStatus?:string; available:boolean }
export interface Review { reviewId:string; sellerId:string; customerId:string; rating:number; comment?:string; createdAt:string; updatedAt?:string }
export interface ReviewSummary { sellerId:string; averageRating:number; reviewCount:number }
export interface Notification { notificationId:string; recipientId:string; type:string; title:string; message:string; referenceId?:string; referenceType?:string; read:boolean; createdAt:string; readAt?:string }
export interface SpringPage<T> { content:T[]; totalElements:number; totalPages:number; number:number; size:number }