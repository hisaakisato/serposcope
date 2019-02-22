require 'json'
require 'aws-sdk-autoscaling'

SERPOSCOPE_HOST = ENV['SERPOSCOPE_HOST']

def lambda_handler(event:, context:)

  instance_id = event['instance_id']
  region = event['region']&.empty? ? 'ap-northeast-1' : event['region']

  autoscaling = Aws::AutoScaling::Client.new({ region: region })
  autoscaling.terminate_instance_in_auto_scaling_group({
    instance_id: instance_id, 
    should_decrement_desired_capacity: false
  })
  puts "Terminate Instance: #{instance_id}"
  
end
