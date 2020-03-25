import json
from typing import Dict
import uuid
import validators
from rdflib import Graph

from openapi_server import query_manager
from openapi_server.settings import ENDPOINT, PREFIX, GRAPH_BASE, UPDATE_ENDPOINT
from openapi_server import logger


def generate_graph(username):
    return "{}{}".format(GRAPH_BASE, username)

def set_up(**kwargs):
    if "username" in kwargs:
        username = kwargs["username"]
    else:
        username = None
    owl_class_name = kwargs["rdf_type_name"]
    resource_type_uri = kwargs["rdf_type_uri"]
    kls = kwargs["kls"]
    return kls, owl_class_name, resource_type_uri, username


def get_resource(**kwargs):
    """
    This method handles a GET METHOD
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """

    #args
    request_args: Dict[str, str] = {}

    if "custom_query_name" in kwargs:
        query_type = kwargs["custom_query_name"]
        return get_resource_custom(request_args=request_args, query_type=query_type, **kwargs)
    else:
        return get_resource_not_custom(request_args=request_args, **kwargs)


def get_resource_custom(query_type, request_args, **kwargs):
    """
    Prepare request for custom queries
    :param query_type:
    :param request_args: contains the values to replaced in the query
    :param kwargs:
    :return:
    """
    if "id" in kwargs:
        return get_one_resource(request_args=request_args, query_type=query_type, **kwargs)
    else:

        if "label" in kwargs and kwargs["label"] is not None:
            query_text = kwargs["label"]
            request_args["label"] = query_text
        return get_all_resource(request_args=request_args, query_type=query_type, **kwargs)


def get_resource_not_custom(request_args, **kwargs):
    """
    Prepare request for not-custom queries

    :param request_args: contains the values to replaced in the query
    :param kwargs:
    :return:
    """
    if "id" in kwargs:
        return get_one_resource(request_args=request_args, query_type="get_one_user", **kwargs)

    else:
        query_type = "get_all_user"
        if "label" in kwargs and kwargs["label"] is not None:
            query_text = kwargs["label"]
            query_type = "get_all_search_user"
            request_args["text"] = query_text
        return get_all_resource(request_args=request_args, query_type=query_type, **kwargs)


def get_one_resource(request_args, query_type="get_one_user", **kwargs):
    """
    Handles a GET method to get one resource
    :param query_type:
    :param request_args:
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    kls, owl_class_name, resource_type_uri, username = set_up(**kwargs)
    request_args["resource"] = build_instance_uri(kwargs["id"])
    if username:
        request_args["g"] = generate_graph(username)
    return request_one(kls, owl_class_name, request_args, resource_type_uri, query_type)


def request_one(kls, owl_class_name, request_args, resource_type_uri, query_type="get_one_user"):
    try:
        response = query_manager.obtain_query(query_directory=owl_class_name,
                                              owl_class_uri=resource_type_uri,
                                              query_type=query_type,
                                              endpoint=ENDPOINT,
                                              request_args=request_args)
        if len(response) > 0:
            return kls.from_dict(response[0])
        else:
            return "Not found", 404, {}

    except:
        logger.error("Exception occurred", exc_info=True)
        return "Bad request", 400, {}


def get_all_resource(request_args, query_type, **kwargs):
    """
    Handles a GET method to get all resource by rdf_type
    :param request_args:
    :param query_type:
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    kls, owl_class_name, resource_type_uri, username = set_up(**kwargs)
    request_args["type"] = resource_type_uri
    if username:
        request_args["g"] = generate_graph(username)
    return request_all(kls, owl_class_name, request_args, resource_type_uri, query_type)


def request_all(kls, owl_class_name, request_args, resource_type_uri, query_type="get_all_user"):
    try:
        response = query_manager.obtain_query(query_directory=owl_class_name,
                                              owl_class_uri=resource_type_uri,
                                              query_type=query_type,
                                              endpoint=ENDPOINT,
                                              request_args=request_args)
        items = []
        for d in response:
            items.append(kls.from_dict(d))
        return items
    except:
        logger.error("Exception occurred", exc_info=True)
        return "Bad request", 400, {}


def put_resource(**kwargs):
    resource_uri = build_instance_uri(kwargs["id"])
    body = kwargs["body"]
    body.id = resource_uri

    try:
        username = kwargs["user"]
    except Exception:
        logger.error("Missing username", exc_info=True)
        return "Bad request: missing username", 400, {}

    '''
    DELETE QUERY
    Since we are updating the resource, we don't want to delete the incoming_relations
    '''

    request_args_delete: Dict[str, str] = {
        "resource": resource_uri,
        "g": generate_graph(username),
        "delete_incoming_relations": False
    }

    try:
        query_manager.delete_query(UPDATE_ENDPOINT, request_args=request_args_delete)
    except:
        logger.error("Exception occurred", exc_info=True)
        return "Error deleting query", 407, {}

    #INSERT QUERY
    body_json = prepare_jsonld(body)
    prefixes, triples = get_insert_query(body_json)
    prefixes = '\n'.join(prefixes)
    triples = '\n'.join(triples)

    request_args: Dict[str, str] = {
        "prefixes": prefixes,
        "triples": triples,
        "g": generate_graph(username)
    }
    if query_manager.insert_query(UPDATE_ENDPOINT, request_args=request_args):
        return body, 201, {}
    else:
        return "Error inserting query", 407, {}


def delete_resource(**kwargs):
    resource_uri = build_instance_uri(kwargs["id"])
    try:
        username = kwargs["user"]
    except Exception:
        logger.error("Missing username", exc_info=True)
        return "Bad request: missing username", 400, {}

    request_args: Dict[str, str] = {
        "resource": resource_uri,
        "g": generate_graph(username),
        "delete_incoming_relations": True
    }
    return query_manager.delete_query(UPDATE_ENDPOINT, request_args=request_args)


def post_resource(**kwargs):
    """
    Post a resource and generate the id
    :param kwargs:
    :type kwargs:
    :return:
    :rtype:
    """
    body = kwargs["body"]
    rdf_type_uri = kwargs["rdf_type_uri"]
    if body.type and rdf_type_uri is not body.type:
        body.type.append(rdf_type_uri)
    else:
        body.type = [rdf_type_uri]
    body.id = generate_new_uri()
    try:
        username = kwargs["user"]
    except Exception:
        logger.error("Missing username", exc_info=True)
        return "Bad request: missing username", 400, {}

    body_json = prepare_jsonld(body)
    prefixes, triples = get_insert_query(body_json)
    prefixes = '\n'.join(prefixes)
    triples = '\n'.join(triples)

    request_args: Dict[str, str] = {
        "prefixes": prefixes,
        "triples": triples,
        "g": generate_graph(username)
    }
    if query_manager.insert_query(UPDATE_ENDPOINT, request_args=request_args):
        return body, 201, {}
    else:
        return "Error inserting query", 407, {}


def get_insert_query(resource_json):
    prefixes = []
    triples = []
    g = Graph().parse(data=resource_json, format='json-ld', publicID=PREFIX)
    s = g.serialize(format='turtle')
    for n in g.namespace_manager.namespaces():
        prefixes.append(f'PREFIX {n[0]}: <{n[1]}>')

    for line in s.decode().split('\n'):
        if not line.startswith('@prefix'):
            triples.append(line)
    return prefixes, triples


def build_instance_uri(uri):
    if validators.url(uri):
        return uri
    return "{}{}".format(PREFIX, uri)

def convert_json_to_triples(body):
    body_json = prepare_jsonld(body)
    prefixes, triples = get_insert_query(body_json)
    prefixes = '\n'.join(prefixes)
    triples = '\n'.join(triples)
    return prefixes, triples

def generate_new_uri():
    return str(uuid.uuid4())

def prepare_jsonld(resource):
    resource_dict = resource.to_dict()
    resource_dict["id"] = build_instance_uri(resource_dict["id"])
    resource_dict['@context'] = query_manager.context
    resource_json = json.dumps(resource_dict)
    return resource_json
