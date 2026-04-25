import { Advertiser } from "./advertiser";
import { Profile } from "./profile";
import { Role } from "./role";

export interface User {
  id: string,
  email: string,
  roles: Role[],
  profile: Profile,
  advertiser: Advertiser
}
