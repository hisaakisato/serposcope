require 'json'
require 'aws-sdk-autoscaling'

MAIN_REGION = 'ap-northeast-1'
MAIN_GROUP = ENV['MAIN_GROUP']
REGIONS = ENV['REGIONS'].split(',')

def lambda_handler(event:, context:)

  autoscaling = Aws::AutoScaling::Client.new({ region: MAIN_REGION })
  resp = autoscaling.describe_auto_scaling_groups({
    auto_scaling_group_names: [ MAIN_GROUP ]
  })
  group = resp.auto_scaling_groups[0]
  diff = group.desired_capacity - group.min_size
  desired_capacity = group.desired_capacity

  REGIONS.each do |region|
    autoscaling = Aws::AutoScaling::Client.new({ region: region })
    resp = autoscaling.describe_auto_scaling_groups({})

    resp.auto_scaling_groups.each do |group|
      capacity = diff == 0 ? group.min_size : desired_capacity
      if capacity != group.desired_capacity
        autoscaling.set_desired_capacity({
          auto_scaling_group_name: group.auto_scaling_group_name,
          desired_capacity: capacity
        })
        puts "Change desired capacity to #{capacity}: #{group.auto_scaling_group_name} (#{region})"
      end
    end
  end
  puts 'Done'
end
