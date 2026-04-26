export interface Advertiser {
  id: string;
  title: string;
  description: string;
  bannerUrl: string;
  avatarUrl: string;
  publicLocation: string;
  publicUrlLocation: string;
  indexed: boolean;
  paid: boolean;
  verified: boolean;
  userId: string;
  email: string;
}

export interface AdvertiserRequest {
  title: string;
  description: string;
  bannerUrl: string;
  avatarUrl: string;
  publicLocation: string;
  publicUrlLocation: string;
  indexed: boolean;
  paid: boolean;
  verified: boolean;
  userId: string;
}
