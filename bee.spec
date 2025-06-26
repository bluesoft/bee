Summary: Bluesoft bee
Name: bee
Version: 1.107
Release: %(perl -e 'print time()')
BuildArch: noarch
AutoReq: no
License: Other
Group: System Environment/Base
URL: http://www.bluesoft.com.br/

BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root
Packager: Bluesoft Fire <devops@bluesoft.com.br>

%description
Bluesoft Bee

%define localdir %(pwd)
%define installdir %{buildroot}/opt

%prep
%setup -cT

%build

cd $RPM_BUILD_DIR
mkdir -p %{installdir} || true

wget -P %{installdir}/ https://github.com/bluesoft/bee/releases/download/%{version}/bee-%{version}.zip
unzip -d %{installdir}/ %{installdir}/bee-%{version}.zip
wget -P %{installdir}/bee-%{version}/lib/ https://s3-sa-east-1.amazonaws.com/bluesoft-sp/install/oracle/ojdbc6-11.2.0.4.0.jar
wget -P %{installdir}/bee-%{version}/lib/ https://s3-sa-east-1.amazonaws.com/bluesoft-sp/install/postgres/postgresql-42.7.4.jar
wget -P %{installdir}/bee-%{version}/lib/ https://s3.amazonaws.com/redshift-downloads/drivers/jdbc/2.1.0.33/redshift-jdbc42-2.1.0.33.jar
wget -P %{installdir}/bee-%{version}/lib/ https://github.com/awslabs/aws-mysql-jdbc/releases/download/1.1.5/aws-mysql-jdbc-1.1.5.jar

%clean
%{__rm} -rf %{buildroot}

%post
ln -sf /opt/bee-%{version}/bin/bee /usr/local/bin/bee

%files
%defattr(-, root, root)
/opt/bee-%{version}
/opt/bee-%{version}.zip
