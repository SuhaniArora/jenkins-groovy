job("kube1_groovy"){
  description("kubernetes job1")
  scm{
    github('SuhaniArora/jenkins-container_kubernetes','master')
  }
  steps{
    shell('sudo cp -vrf * /root/projects/jenkins')
  }
  triggers{
    gitHubPushTrigger()
  }
}

job("kube2_groovy"){
  steps{
    shell('''
	if sudo ls /root/projects/jenkins | grep php
      	then
		if sudo kubectl get deployment --selector "app in (httpd)" | grep httpd-web
    		then
			sudo kubectl apply -f /root/projects/jenkins/webserver.yml
           		POD=$(sudo kubectl get pod -l app=httpd -o jsonpath="{.items[0].metadata.name}")
        		echo $POD
        		sudo kubectl cp /root/projects/jenkins/index.php $POD:/var/www/html
		else
    			if ! kubectl get pvc | grep httpdweb1-pv-claim
        		then
            			sudo kubectl create -f /root/projects/jenkins/pvc.yml
        		fi
        		sudo kubectl create -f /root/projects/jenkins/webserver.yml
        		POD=$(sudo kubectl get pod -l app=httpd -o jsonpath="{.items[0].metadata.name}")
        		echo $POD
        		sudo kubectl cp /root/projects/jenkins/index.php $POD:/var/www/html
    		fi
   	fi
	''')
  }
  triggers {
        upstream('kube1_groovy', 'SUCCESS')
  }
}

job("kube3_groovy")
{
  steps{
    shell('''
status=$(curl -o /dev/null -s -w "%{http_code}" http://192.168.99.100:30002)
if [[ $status == 200 ]]
then
    echo "Running"
    exit 0
else
	pod_name=$(kubectl get pod -o='name' | grep jenkinsmaster )
    pod_ip=$(kubectl get ${pod_name} --template={{.status.podIP}})
    curl --user admin:redhat http://${pod_ip}:8080/job/j4/build?token=mail
fi
     ''')
  }
  
  triggers {
        upstream('kube2_groovy', 'SUCCESS')
  }
  
}

job("kube4_groovy")
{
  steps{
    shell(
      'python3 /root/projects/jenkins/mail.py'
    )
  }
   triggers {
        upstream('kube3_groovy', 'UNSTABLE')
  }
}
