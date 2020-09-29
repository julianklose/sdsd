![SDSD](website/src/main/resources/img/sdsd-logo.png)

# SDSD

Smart Data - Smart Services

Modern agricultural technology and the increasing digitalisation of such processes provide a wide range of data. 
However, their efficient and beneficial use suffers from legitimate concerns about data sovereignty and control, format inconsistencies and different interpretations. 
As a proposed solution, we present [Wikinormia](sdsd-api), a collaborative platform in which interested participants can describe and discuss their own new data formats. 
Once a finalized vocabulary has been created, specific [parsers](parser) can semantically process the raw data into three basic representations: spatial information, time series and semantic facts (agricultural knowledge graph). 
Thanks to publicly accessible definitions and descriptions, developers can easily gain an overview of the concepts that are relevant to them. 
A variety of services will then (subject to individual access rights) be able to query their data simply via a [query interface](website/src/main/java/de/sdsd/projekt/prototype/jsonrpc/ApiEndpoint.java#L82) and retrieve results (by using JSON-RPC).
We have implemented this proposed solution in a prototype in the SDSD project which has been made open-source here.

Visit our german [project homepage](http://www.sdsd-projekt.de/) for more information.

A [paper](https://gil-net.de/wp-content/uploads/2020/02/GIL_2020FK_LNI-gesamt.pdf#page=133) about this topic can be cited with this bibtex:
```
@article{klose2020datenaufbereitung,
  title={Datenaufbereitung in der Landwirtschaft durch automatisierte semantische Annotation},
  author={Klose, Julian and Schr{\"o}der, Markus and Becker, Silke and Bernardi, Ansgar and Ruckelshausen, Arno},
  journal={40. GIL-Jahrestagung, Digitalisierung f{\"u}r Mensch, Umwelt und Tier},
  year={2020},
  publisher={Gesellschaft f{\"u}r Informatik eV}
}
```
There is also an [English version of the paper on arxiv](https://arxiv.org/abs/1911.06606).
