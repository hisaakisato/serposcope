require 'json'
require 'aws-sdk-ec2'
require 'aws-sdk-secretsmanager'
require 'mysql2'

SERPOSCOPE_HOST = ENV['SERPOSCOPE_HOST']

def lambda_handler(event:, context:)

  json = JSON.parse(event['Records'][0]['Sns']['Message'])
  availability_zone = json["Details"]['Availability Zone']
  region = availability_zone.chop
  endpoint = ENV['EC2_ENDPOINT_' + region.upcase.gsub('-', '_')]

  instance_id = json['EC2InstanceId']
  cause = json['Cause']

  ec2 = Aws::EC2::Resource.new({ region: region, endpoint: endpoint })
  instance = ec2.instance(instance_id)
  private_ip_address = instance.private_ip_address
  public_ip_address = instance.public_ip_address

  event_type = json['Event']
  if event_type == 'autoscaling:EC2_INSTANCE_LAUNCH'
    add_proxy(instance_id, region, private_ip_address, public_ip_address)
  else
    delete_proxy(instance_id)
  end
  
end

def mysql_client
  secretsmanager = Aws::SecretsManager::Client.new()
  username = secretsmanager.get_secret_value({
    secret_id: "serposcope/database/user"
  }).secret_string
  password = secretsmanager.get_secret_value({
    secret_id: "serposcope/database/password"
  }).secret_string
  Mysql2::Client.new(
    :host => SERPOSCOPE_HOST,
    :username => username,
    :password => password,
    :encoding => 'utf8',
    :database => 'serposcope')
end

def add_proxy(instance_id, region, private_ip_address, public_ip_address)

  begin
    client = mysql_client
    client.query(
      "INSERT INTO PROXY (type, ip, port, last_check, status, remote_ip, instance_id, region) " \
      "VALUES (1, '#{private_ip_address}', 3128, now(), 1, '#{public_ip_address}', '#{instance_id}', '#{region}') " \
      "ON DUPLICATE KEY UPDATE last_check=now(), status=1")
    puts "Proxy addedd: instanceId: #{instance_id} IP: #{private_ip_address} PublicIP: #{public_ip_address}"
  ensure
    client.close
  end

end

def delete_proxy(instance_id)

  begin
    client = mysql_client
    client.query(
      "DELETE FROM PROXY WHERE instance_id='#{instance_id}'")
    puts "Proxy removed: instanceId: #{instance_id}"
  ensure
    client.close
  end

end
