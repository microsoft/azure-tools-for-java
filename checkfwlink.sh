#!/bin/bash
FWLINKBASE="http://go.microsoft.com/fwlink/?LinkID="
LINKIDLIST=("286720" "620956" "512749" "526878" "252838" "255555" "255557" "267429" "400838" "723585" "722349&clcid=0x409" "824704" "847862")

result=()

function check() {
	url="${FWLINKBASE}$1"
	echo ${url}
	respcode=`curl -L -i -o /dev/null --silent -w "\n%{http_code}" ${url}`
	if [ $? -ne 0 ]; then
		echo "Invalid URL ${url} with error"
		result=(${result[@]} ${url})
	elif [ "${respcode}" -ne 200 ]; then
		echo "Invalid URL ${url} with code ${respcode}"
		result=(${result[@]} ${url})
	fi
}


for id in ${LINKIDLIST[@]}
do
	check ${id}
done
if [ ${#result[@]} -ne 0 ]; then
	content=`printf ",%s" ${result[@]}`
	content="Found ${#result[@]} invalid URLS${content}";
	echo ${content}
	exit ${#result[@]}
else
	echo "All fw link is available"
	exit 0
fi
