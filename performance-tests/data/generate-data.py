import uuid
import csv
import time

# Constants
USER_COUNT = 2000
PASSWORD = "123456789"
PASSWORD_HASH = "$2a$12$WQ4q./eFJcOsEo7cCxW.E.umgVFYPS73G1nImfesTMAAD.Bj.joCu"
IMAGE_URL = ""
ROLE_NAME = "USER"
ROLE_DESC = "Standard user role with basic access"

def generate_uuid_v7():
    """
    Simplified UUID v7 implementation.
    UUID v7: 48-bit timestamp (ms) | 4-bit version (7) | 12-bit random | 2-bit variant (10) | 62-bit random
    """
    # Timestamp in milliseconds
    ms = int(time.time() * 1000)
    
    # 48 bits of timestamp
    timestamp_bits = ms & 0xFFFFFFFFFFFF
    
    # Random bits
    rand_bits = uuid.uuid4().int
    
    # Construct UUID v7
    # bits 0-47: timestamp
    # bits 48-51: version (0111 = 7)
    # bits 52-63: random
    # bits 64-65: variant (10)
    # bits 66-127: random
    
    v7_int = (timestamp_bits << 80) | \
             (7 << 76) | \
             ((rand_bits >> 64) & 0xFFF) << 64 | \
             (2 << 62) | \
             (rand_bits & 0x3FFFFFFFFFFFFFFF)
             
    return str(uuid.UUID(int=v7_int))

def generate_data():
    # Generate CLIENT_ID (UUID v7)
    client_id = generate_uuid_v7()
    
    # Update .env and lib/config.ts
    with open("performance-tests/.env", "w") as f:
        f.write(f"BASE_URL=http://localhost:8080\nCLIENT_ID={client_id}\n")
    
    with open("performance-tests/lib/config.ts", "w") as f:
        f.write(f"export const CONFIG = {{\n  baseURL: 'http://localhost:8080',\n  clientId: '{client_id}', \n userAgent: 'Mozilla v1'\n}};")

    sql_lines = []
    csv_data = []

    # Use the official USER role ID from V2__seed_security_data.sql
    role_id = '019e7749-5281-7115-9cd2-7363b0d205a0'

    # 4. Create Users, Profiles, and Advertisers
    for i in range(1, USER_COUNT + 1):
        
        email = f"perf_user_{i}@example.com"
        
        # User record
        user_id = str(generate_uuid_v7())
        sql_lines.append(
            f"INSERT INTO users (id, email, password, enable, account_non_expired, account_non_locked, credential_non_expired, email_verified, google_connected) "
            f"VALUES ('{user_id}', '{email}', '{PASSWORD_HASH}', true, true, true, true, true, false);"
        )
        
        # User Role record
        user_role_id = str(generate_uuid_v7())
        sql_lines.append(f"INSERT INTO user_roles (id, user_id, role_id) VALUES ('{user_role_id}', '{user_id}', '{role_id}');")
        
        # Profile record
        profile_id = str(generate_uuid_v7())
        sql_lines.append(
            f"INSERT INTO profiles (id, first_name, last_name, address, avatar_url, user_id) "
            f"VALUES ('{profile_id}', 'First_{i}', 'Last_{i}', 'Address_{i}', '{IMAGE_URL}', '{user_id}');"
        )
        
        # Advertiser record
        adv_id = str(generate_uuid_v7())
        sql_lines.append(
            f"INSERT INTO advertisers (id, title, description, banner_url, avatar_url, public_location, public_url_location, indexed, paid, verified, user_id) "
            f"VALUES ('{adv_id}', 'Advertiser_{i}', 'Description_{i}', '{IMAGE_URL}', '{IMAGE_URL}', 'Loc_{i}', 'http://loc_{i}.com', true, false, true, '{user_id}');"
        )
        
        # CSV data
        csv_data.append([email, PASSWORD])

    # Write SQL file
    with open("performance-tests/data/seeding-users.sql", "w") as f:
        f.write("\n".join(sql_lines))


    # Write CSV file
    with open("performance-tests/data/users.csv", "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["email", "password"])
        writer.writerows(csv_data)

    print(f"Successfully generated seeding-users.sql, users.csv, and updated config with CLIENT_ID {client_id}.")

if __name__ == "__main__":
    generate_data()